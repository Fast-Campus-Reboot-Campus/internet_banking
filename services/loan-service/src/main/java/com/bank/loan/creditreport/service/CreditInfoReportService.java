package com.bank.loan.creditreport.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.creditreport.domain.CreditInfoReport;
import com.bank.loan.creditreport.dto.AdminCreditInfoReportListResponse;
import com.bank.loan.creditreport.dto.CreditInfoReportListResponse;
import com.bank.loan.creditreport.dto.AckCallbackRequest;
import com.bank.loan.creditreport.dto.CreditInfoReportResponse;
import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.outbox.CreditInfoReportOutbox;
import com.bank.loan.creditreport.outbox.CreditInfoReportOutboxRepository;
import com.bank.loan.creditreport.repository.CreditInfoReportRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 신용정보 신고 (KCB/NICE) 서비스.
 *
 * 본 단계: submit → 신고 row REQUESTED + outbox row PENDING 한 트랜잭션 적재. 외부 호출은 하지 않는다
 * (AI_GUIDELINES: 트랜잭션 내 외부 API 금지). dispatch 배치(별 plan)가 outbox 를 폴링해 어댑터를 호출하고
 * 결과를 받아 SENT/FAILED 로 전이한다.
 *
 * status_history: REPORT_REQUESTED 만 발생. SENT/FAILED/ACKED 전이는 dispatch 측에서 별도 publish.
 */
@Service
@RequiredArgsConstructor
public class CreditInfoReportService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "CREDIT_INFO_REPORT";
    private static final String REASON_REQUESTED = "REPORT_REQUESTED";
    private static final String REASON_ACKED = "REPORT_ACKED";
    private static final String REASON_REQUEUED = "REPORT_REQUEUED";

    private final CreditInfoReportRepository repository;
    private final CreditInfoReportOutboxRepository outboxRepository;
    private final LoanContractRepository contractRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public CreditInfoReportResponse submit(Long cntrId, SubmitReportRequest req) {
        return submit(cntrId, null, req);
    }

    /**
     * 자동 발화용 오버로드.
     *
     * dlqId 가 주어지면 (cntrId, dlqId, crptTypeCd, reportReasonCd) 멱등 키로 중복 신고를 막는다.
     * 이미 활성(REQUESTED/SENT/ACKED) 신고가 있으면 기존 row 를 반환 — FAILED/DEAD 는 재발화 허용.
     * race 충돌은 UNIQUE 인덱스가 차단 — 호출자는 신경 쓰지 않는다.
     */
    @Transactional
    public CreditInfoReportResponse submit(Long cntrId, Long dlqId, SubmitReportRequest req) {
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        if (dlqId != null) {
            Optional<CreditInfoReport> existing = repository
                    .findFirstByCntrIdAndDlqIdAndCrptTypeCdAndReportReasonCdAndCrptStatusCdInAndDeletedAtIsNullOrderByCrptIdAsc(
                            cntrId, dlqId, req.reportTypeCd(), req.reportReasonCd(),
                            List.of(CreditInfoReport.STATUS_REQUESTED,
                                    CreditInfoReport.STATUS_SENT,
                                    CreditInfoReport.STATUS_ACKED));
            if (existing.isPresent()) {
                return CreditInfoReportResponse.of(existing.get());
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        CreditInfoReport saved = repository.save(CreditInfoReport.builder()
                .cntrId(contract.getCntrId())
                .dlqId(dlqId)
                .customerId(contract.getCustomerId())
                .crptTypeCd(req.reportTypeCd())
                .crptAgencyCd(req.agencyCd())
                .crptStatusCd(CreditInfoReport.STATUS_REQUESTED)
                .reportTargetCd(req.reportTargetCd())
                .reportReasonCd(req.reportReasonCd())
                .reportPayload(req.reportPayload())
                .build());
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getCrptId(),
                null, CreditInfoReport.STATUS_REQUESTED,
                REASON_REQUESTED,
                "type=" + req.reportTypeCd() + " / agency=" + req.agencyCd(),
                actorId
        ));

        // outbox row 적재 — dispatch 배치가 픽업할 대상.
        outboxRepository.save(CreditInfoReportOutbox.builder()
                .crptId(saved.getCrptId())
                .status(CreditInfoReportOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(CreditInfoReportOutbox.DEFAULT_MAX_ATTEMPT)
                .nextAttemptAt(now)
                .build());

        return CreditInfoReportResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public CreditInfoReportListResponse list(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        List<CreditInfoReportResponse> items = repository
                .findByCntrIdAndDeletedAtIsNullOrderByCreatedAtAsc(cntrId)
                .stream()
                .map(CreditInfoReportResponse::of)
                .toList();
        return CreditInfoReportListResponse.of(cntrId, items);
    }

    /**
     * 외부 기관 ACK callback 처리. SENT → ACKED.
     *
     * 이미 ACKED → 멱등(같은 row 반환).
     * SENT 가 아닌 다른 상태 → LOAN_151.
     */
    @Transactional
    public CreditInfoReportResponse ack(Long crptId, AckCallbackRequest req) {
        CreditInfoReport report = repository.findByCrptIdAndDeletedAtIsNull(crptId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_150));

        if (CreditInfoReport.STATUS_ACKED.equals(report.currentStatus())) {
            return CreditInfoReportResponse.of(report); // 멱등
        }
        if (!CreditInfoReport.STATUS_SENT.equals(report.currentStatus())) {
            throw new BusinessException(LoanErrorCode.LOAN_151,
                    "current=" + report.currentStatus());
        }

        String before = report.currentStatus();
        report.markAcked(req.ackedAt(), req.externalAckNo());
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, report.getCrptId(),
                before, CreditInfoReport.STATUS_ACKED,
                REASON_ACKED, "externalAckNo=" + req.externalAckNo(),
                currentActor.currentActorId()
        ));
        return CreditInfoReportResponse.of(report);
    }

    /**
     * 운영자 재전송. ACKED → LOAN_152. 그 외 상태(REQUESTED/SENT/FAILED/DEAD) → outbox requeue.
     *
     * outbox row 가 없는 신고(legacy)는 새로 만들어 PENDING 으로 적재한다.
     * 신고 row 는 markRequeued 로 REQUESTED 복귀, externalTxNo/reportedAt 초기화.
     */
    @Transactional
    public CreditInfoReportResponse retry(Long crptId) {
        CreditInfoReport report = repository.findByCrptIdAndDeletedAtIsNull(crptId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_150));

        if (CreditInfoReport.STATUS_ACKED.equals(report.currentStatus())) {
            throw new BusinessException(LoanErrorCode.LOAN_152);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();
        String before = report.currentStatus();
        report.markRequeued();
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, report.getCrptId(),
                before, CreditInfoReport.STATUS_REQUESTED,
                REASON_REQUEUED, "operator retry", actorId
        ));

        CreditInfoReportOutbox outbox = outboxRepository
                .findByCrptIdAndDeletedAtIsNull(crptId).orElse(null);
        if (outbox == null) {
            outboxRepository.save(CreditInfoReportOutbox.builder()
                    .crptId(crptId)
                    .status(CreditInfoReportOutbox.STATUS_PENDING)
                    .attemptNo(0)
                    .maxAttempt(CreditInfoReportOutbox.DEFAULT_MAX_ATTEMPT)
                    .nextAttemptAt(now)
                    .build());
        } else {
            outbox.requeue(now);
        }
        return CreditInfoReportResponse.of(report);
    }

    @Transactional(readOnly = true)
    public AdminCreditInfoReportListResponse listAll(String statusCd, Pageable pageable) {
        Page<CreditInfoReportResponse> page;
        if (StringUtils.hasText(statusCd)) {
            page = repository.findAllByCrptStatusCdAndDeletedAtIsNullOrderByCreatedAtDesc(statusCd, pageable)
                    .map(CreditInfoReportResponse::of);
        } else {
            page = repository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable)
                    .map(CreditInfoReportResponse::of);
        }
        return AdminCreditInfoReportListResponse.of(page);
    }

    @Transactional(readOnly = true)
    public CreditInfoReportResponse getById(Long crptId) {
        return repository.findByCrptIdAndDeletedAtIsNull(crptId)
                .map(CreditInfoReportResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_150));
    }
}
