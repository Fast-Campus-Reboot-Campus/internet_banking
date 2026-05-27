package com.bank.loan.notification.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.notification.channel.NotificationChannelAdapter;
import com.bank.loan.notification.channel.NotificationChannelRegistry;
import com.bank.loan.notification.dto.NotificationDispatchSummary;
import com.bank.loan.notification.outbox.NotificationOutbox;
import com.bank.loan.notification.outbox.NotificationOutboxRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 알림 outbox 디스패치 배치.
 *
 * 트랜잭션 모델 (AI_GUIDELINES: 트랜잭션 내 외부 API 금지):
 *   1) 후보 outbox 픽업 — 자체 readonly 트랜잭션
 *   2) 어댑터 호출은 트랜잭션 밖
 *   3) 결과 적용은 짧은 REQUIRES_NEW 트랜잭션 (outbox + status_history 동기)
 *
 * 페이지 200 — findAll 무페이지 금지.
 * 한 row 실패가 전체 배치를 깨면 안 된다 — row 단위 try/catch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    public static final int DEFAULT_PAGE_SIZE = 200;

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "NOTIFICATION_OUTBOX";
    private static final String REASON_SENT   = "NOTI_SENT";
    private static final String REASON_FAILED = "NOTI_FAILED";
    private static final String REASON_DEAD   = "NOTI_DEAD";

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationChannelRegistry channelRegistry;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final PlatformTransactionManager txManager;

    private TransactionTemplate perRowWriter;

    @PostConstruct
    void init() {
        this.perRowWriter = new TransactionTemplate(txManager);
        this.perRowWriter.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 5초마다 outbox 폴링 — PENDING/FAILED row 를 채널 어댑터로 발송.
     * fixedDelay: 이전 실행 완료 후 5초 대기 (동시 실행 방지).
     */
    @Scheduled(fixedDelay = 5_000)
    public void scheduledDispatch() {
        NotificationDispatchSummary result = dispatch(DEFAULT_PAGE_SIZE);
        if (result.processed() > 0) {
            log.info("[noti-dispatch] scheduled: total={} sent={} failed={} dead={}",
                    result.processed(), result.sent(), result.failed(), result.dead());
        }
    }

    public NotificationDispatchSummary dispatch() {
        return dispatch(DEFAULT_PAGE_SIZE);
    }

    public NotificationDispatchSummary dispatch(int pageSize) {
        OffsetDateTime now = OffsetDateTime.now();
        List<NotificationOutbox> candidates = pickCandidates(now, pageSize);
        int sent = 0, failed = 0, dead = 0;
        for (NotificationOutbox row : candidates) {
            try {
                Outcome o = processOne(row.getOutboxId());
                switch (o) {
                    case SENT   -> sent++;
                    case FAILED -> failed++;
                    case DEAD   -> dead++;
                    case SKIP   -> { /* 상태 이미 바뀜 — 카운트 제외 */ }
                }
            } catch (RuntimeException e) {
                log.warn("[noti-dispatch] outboxId={} skipped due to error: {}",
                        row.getOutboxId(), e.getMessage());
            }
        }
        return NotificationDispatchSummary.of(candidates.size(), sent, failed, dead);
    }

    @Transactional(readOnly = true)
    protected List<NotificationOutbox> pickCandidates(OffsetDateTime now, int pageSize) {
        return outboxRepository
                .findByStatusInAndNextAttemptAtLessThanEqualAndDeletedAtIsNullOrderByNextAttemptAtAsc(
                        List.of(NotificationOutbox.STATUS_PENDING, NotificationOutbox.STATUS_FAILED),
                        now,
                        PageRequest.of(0, pageSize));
    }

    private Outcome processOne(Long outboxId) {
        NotificationOutbox snapshot = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(outboxId).orElse(null);
        if (snapshot == null) return Outcome.SKIP;
        if (!NotificationOutbox.STATUS_PENDING.equals(snapshot.getStatus())
                && !NotificationOutbox.STATUS_FAILED.equals(snapshot.getStatus())) {
            return Outcome.SKIP;
        }

        NotificationChannelAdapter adapter = channelRegistry.resolve(snapshot.getChannelCd());

        NotificationChannelAdapter.SendResult sendResult;
        try {
            sendResult = adapter.send(snapshot);
        } catch (RuntimeException e) {
            sendResult = new NotificationChannelAdapter.SendResult(false, null, "EXC", safeMsg(e));
        }
        final NotificationChannelAdapter.SendResult result = sendResult;

        boolean ok = Boolean.TRUE.equals(perRowWriter.execute(s -> applyResult(outboxId, result)));
        return ok ? Outcome.SENT : applyFailedOutcome(outboxId);
    }

    private Boolean applyResult(Long outboxId, NotificationChannelAdapter.SendResult result) {
        NotificationOutbox row = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(outboxId).orElseThrow();
        Long actorId = currentActor.currentActorId();
        OffsetDateTime now = OffsetDateTime.now();
        String before = row.getStatus();

        if (result.success()) {
            row.markSent(now);
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, row.getOutboxId(),
                    before, NotificationOutbox.STATUS_SENT,
                    REASON_SENT, "providerMsgId=" + result.providerMsgId(), actorId
            ));
            return true;
        }

        row.markFailed(result.responseMessage(), now);
        if (NotificationOutbox.STATUS_DEAD.equals(row.getStatus())) {
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, row.getOutboxId(),
                    before, NotificationOutbox.STATUS_DEAD,
                    REASON_DEAD, "lastError=" + truncate(result.responseMessage(), 200), actorId
            ));
        } else {
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, row.getOutboxId(),
                    before, NotificationOutbox.STATUS_FAILED,
                    REASON_FAILED, "lastError=" + truncate(result.responseMessage(), 200), actorId
            ));
        }
        return false;
    }

    private Outcome applyFailedOutcome(Long outboxId) {
        NotificationOutbox row = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(outboxId).orElseThrow();
        return NotificationOutbox.STATUS_DEAD.equals(row.getStatus())
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
