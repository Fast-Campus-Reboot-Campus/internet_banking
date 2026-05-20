package com.bank.loan.creditreport.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.creditreport.domain.CreditInfoReport;
import com.bank.loan.creditreport.dto.CreditInfoReportListResponse;
import com.bank.loan.creditreport.dto.CreditInfoReportResponse;
import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.repository.CreditInfoReportRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 신용정보 신고 (KCB/NICE) 서비스.
 *
 * 본 단계: 등록 → 즉시 외부 전송(stub) → SENT 한 트랜잭션 안에서 전이.
 *   external_tx_no 자체 채번 (idempotency·추적 키).
 *   reported_at = 전송 시각.
 *   ACK callback / 재전송 / 실패 처리는 후속.
 *
 * status_history: REPORT_REQUESTED → REPORT_SENT.
 */
@Service
@RequiredArgsConstructor
public class CreditInfoReportService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "CREDIT_INFO_REPORT";
    private static final String REASON_REQUESTED = "REPORT_REQUESTED";
    private static final String REASON_SENT = "REPORT_SENT";

    private final CreditInfoReportRepository repository;
    private final LoanContractRepository contractRepository;
    private final ExternalTxNumberGenerator txNoGenerator;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public CreditInfoReportResponse submit(Long cntrId, SubmitReportRequest req) {
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        CreditInfoReport saved = repository.save(CreditInfoReport.builder()
                .cntrId(contract.getCntrId())
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

        // 외부 전송(stub) — 본 단계는 항상 SUCCESS
        String externalTxNo = txNoGenerator.generate(now);
        saved.markSent(externalTxNo, now);
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getCrptId(),
                CreditInfoReport.STATUS_REQUESTED, CreditInfoReport.STATUS_SENT,
                REASON_SENT, "externalTxNo=" + externalTxNo, actorId
        ));

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

    @Transactional(readOnly = true)
    public CreditInfoReportResponse getById(Long crptId) {
        return repository.findByCrptIdAndDeletedAtIsNull(crptId)
                .map(CreditInfoReportResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_150));
    }
}
