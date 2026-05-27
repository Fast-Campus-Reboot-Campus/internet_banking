package com.bank.loan.creditreport.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.creditreport.channel.CreditInfoReportChannelAdapter;
import com.bank.loan.creditreport.channel.CreditInfoReportChannelRegistry;
import com.bank.loan.creditreport.domain.CreditInfoReport;
import com.bank.loan.creditreport.dto.CreditInfoReportDispatchSummary;
import com.bank.loan.creditreport.outbox.CreditInfoReportOutbox;
import com.bank.loan.creditreport.outbox.CreditInfoReportOutboxRepository;
import com.bank.loan.creditreport.repository.CreditInfoReportRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 신용정보 신고 outbox 디스패치 배치.
 *
 * 호출 시점:
 *   - 운영자 수동 (`POST /api/internal/credit-info-reports/dispatch`)
 *   - 분 단위 스케줄러 (구성은 별 plan — 본 단계는 엔드포인트만 제공)
 *
 * 트랜잭션 모델 (AI_GUIDELINES: 트랜잭션 내 외부 API 금지):
 *   1) 후보 outbox 픽업 — 자체 readonly 트랜잭션
 *   2) for each row: 어댑터 호출은 **트랜잭션 밖**
 *   3) 결과 수신 후 짧은 REQUIRES_NEW 트랜잭션으로 outbox + 신고 row + status_history 동기 갱신
 *
 * 한 row 처리 실패가 전체 배치를 깨면 안 된다 — try/catch 로 row 단위 격리.
 * 페이지 크기 200 — findAll 무페이지 금지.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditInfoReportDispatchService {

    public static final int DEFAULT_PAGE_SIZE = 200;

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "CREDIT_INFO_REPORT";
    private static final String REASON_SENT   = "REPORT_SENT";
    private static final String REASON_FAILED = "REPORT_FAILED";
    private static final String REASON_DEAD   = "REPORT_DEAD";

    private final CreditInfoReportOutboxRepository outboxRepository;
    private final CreditInfoReportRepository reportRepository;
    private final CreditInfoReportChannelRegistry channelRegistry;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final PlatformTransactionManager txManager;

    private TransactionTemplate perRowWriter;

    @PostConstruct
    void init() {
        this.perRowWriter = new TransactionTemplate(txManager);
        this.perRowWriter.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CreditInfoReportDispatchSummary dispatch() {
        return dispatch(DEFAULT_PAGE_SIZE);
    }

    public CreditInfoReportDispatchSummary dispatch(int pageSize) {
        OffsetDateTime now = OffsetDateTime.now();
        List<CreditInfoReportOutbox> candidates = pickCandidates(now, pageSize);
        int sent = 0;
        int failed = 0;
        int dead = 0;
        for (CreditInfoReportOutbox candidate : candidates) {
            try {
                Outcome o = processOne(candidate.getOutboxId());
                switch (o) {
                    case SENT   -> sent++;
                    case FAILED -> failed++;
                    case DEAD   -> dead++;
                    case SKIP   -> { /* 상태가 이미 바뀌었음 — 카운트 제외 */ }
                }
            } catch (RuntimeException e) {
                // row 단위 예외는 전체 배치 진행을 막지 않는다 — 다음 배치에서 재시도.
                log.warn("[creditreport-dispatch] outboxId={} skipped due to error: {}",
                        candidate.getOutboxId(), e.getMessage());
            }
        }
        return CreditInfoReportDispatchSummary.of(candidates.size(), sent, failed, dead);
    }

    @Transactional(readOnly = true)
    protected List<CreditInfoReportOutbox> pickCandidates(OffsetDateTime now, int pageSize) {
        return outboxRepository
                .findByStatusInAndNextAttemptAtLessThanEqualAndDeletedAtIsNullOrderByNextAttemptAtAsc(
                        List.of(CreditInfoReportOutbox.STATUS_PENDING,
                                CreditInfoReportOutbox.STATUS_FAILED),
                        now,
                        PageRequest.of(0, pageSize));
    }

    /**
     * 한 outbox row 처리. 어댑터 호출은 트랜잭션 밖, 결과 적용만 REQUIRES_NEW 안.
     */
    private Outcome processOne(Long outboxId) {
        CreditInfoReportOutbox snapshot = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(outboxId).orElse(null);
        if (snapshot == null) return Outcome.SKIP;
        if (!CreditInfoReportOutbox.STATUS_PENDING.equals(snapshot.getStatus())
                && !CreditInfoReportOutbox.STATUS_FAILED.equals(snapshot.getStatus())) {
            return Outcome.SKIP;
        }

        CreditInfoReport report = reportRepository
                .findByCrptIdAndDeletedAtIsNull(snapshot.getCrptId()).orElse(null);
        if (report == null) return Outcome.SKIP;

        CreditInfoReportChannelAdapter adapter = channelRegistry.resolve(report.getCrptAgencyCd());

        CreditInfoReportChannelAdapter.SendResult sendResult;
        try {
            sendResult = adapter.send(report);
        } catch (RuntimeException e) {
            sendResult = new CreditInfoReportChannelAdapter.SendResult(false, null, "EXC", safeMsg(e));
        }
        final CreditInfoReportChannelAdapter.SendResult result = sendResult;

        boolean ok = Boolean.TRUE.equals(perRowWriter.execute(s -> applyResult(outboxId, result)));
        return ok ? Outcome.SENT : applyFailedOutcome(outboxId);
    }

    /**
     * 결과 적용. 성공 시 true 반환. 실패 시 false 반환 — 호출자가 outbox 의 status 를 다시 읽어
     * FAILED/DEAD 를 구분한다.
     */
    private Boolean applyResult(Long outboxId, CreditInfoReportChannelAdapter.SendResult result) {
        CreditInfoReportOutbox row = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(outboxId).orElseThrow();
        CreditInfoReport report = reportRepository
                .findByCrptIdAndDeletedAtIsNull(row.getCrptId()).orElseThrow();
        Long actorId = currentActor.currentActorId();
        OffsetDateTime now = OffsetDateTime.now();

        if (result.success()) {
            String before = report.currentStatus();
            report.markSent(result.externalTxNo(), now);
            row.markSent(now);
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, report.getCrptId(),
                    before, CreditInfoReport.STATUS_SENT,
                    REASON_SENT, "externalTxNo=" + result.externalTxNo(), actorId
            ));
            return true;
        }

        // 실패 처리: outbox 가 attemptNo++ 와 DEAD 도달 여부를 판단해준다.
        row.markFailed(result.responseMessage(), now);
        String before = report.currentStatus();
        if (CreditInfoReportOutbox.STATUS_DEAD.equals(row.getStatus())) {
            report.markDead();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, report.getCrptId(),
                    before, CreditInfoReport.STATUS_DEAD,
                    REASON_DEAD, "lastError=" + truncate(result.responseMessage(), 200), actorId
            ));
        } else {
            report.markFailed();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, report.getCrptId(),
                    before, CreditInfoReport.STATUS_FAILED,
                    REASON_FAILED, "lastError=" + truncate(result.responseMessage(), 200), actorId
            ));
        }
        return false;
    }

    private Outcome applyFailedOutcome(Long outboxId) {
        CreditInfoReportOutbox row = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(outboxId).orElseThrow();
        return CreditInfoReportOutbox.STATUS_DEAD.equals(row.getStatus())
                ? Outcome.DEAD : Outcome.FAILED;
    }

    private static String safeMsg(Throwable t) {
        return t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private enum Outcome { SENT, FAILED, DEAD, SKIP }
}
