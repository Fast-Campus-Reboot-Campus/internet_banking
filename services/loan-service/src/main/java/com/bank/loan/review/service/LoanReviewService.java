package com.bank.loan.review.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.dsr.domain.DsrCalculation;
import com.bank.loan.dsr.repository.DsrCalculationRepository;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.RunReviewRequest;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 본심사(Underwriting) 서비스 — flows §1.1, §2.1 의 "REVIEWING → APPROVED/REJECTED".
 *
 * 흐름:
 *   1) 신청 존재 검증 (LOAN_012)
 *   2) 중복 본심사 차단 (LOAN_039, appl_id UNIQUE)
 *   3) 사전조건: PRESCREENED 상태 + CB(APPROVE/REVIEW) + DSR PASS (LOAN_038)
 *   4) APPROVED:
 *        approved_amount/rate/period 자동 산정 (입력값 우선)
 *        신청 → APPROVED, approved_at 기록
 *   5) REJECTED:
 *        reject_reason_cd 기록
 *        신청 → REJECTED
 *   6) status_history 양쪽 (LOAN_REVIEW null→COMPLETED, LOAN_APPLICATION PRESCREENED→다음)
 */
@Service
@RequiredArgsConstructor
public class LoanReviewService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_REVIEW = "LOAN_REVIEW";
    private static final String TARGET_APPLICATION = "LOAN_APPLICATION";
    private static final String REASON_REVIEW_APPROVED = "REVIEW_APPROVED";
    private static final String REASON_REVIEW_REJECTED = "REVIEW_REJECTED";

    private final LoanReviewRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final DsrCalculationRepository dsrCalculationRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public LoanReviewResponse run(Long applId, RunReviewRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_039);
        }

        // 사전조건 1: 신청 상태 PRESCREENED
        if (!application.isReviewable()) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "current=" + application.currentStatus());
        }

        // 사전조건 2: CB 완료 + decision != REJECT
        CreditEvaluation ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "credit-evaluation required"));
        if (CreditEvaluation.DECISION_REJECT.equals(ceval.getCevalDecisionCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "cevalDecision=REJECT");
        }

        // 사전조건 3: DSR PASS
        DsrCalculation dsr = dsrCalculationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "dsr-calculation required"));
        if (!DsrCalculation.STATUS_PASS.equals(dsr.getDsrStatusCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "dsrStatus=" + dsr.getDsrStatusCd());
        }

        boolean approved = LoanReview.DECISION_APPROVED.equals(req.revDecisionCd());
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long approvedAmount = null;
        Integer approvedRate = null;
        Integer approvedPeriod = null;
        OffsetDateTime approvedAt = null;
        if (approved) {
            LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                    .orElse(null);
            approvedAmount = req.approvedAmount() != null
                    ? req.approvedAmount()
                    : determineApprovedAmount(application, ceval, product);
            approvedRate = req.approvedRateBps() != null
                    ? req.approvedRateBps()
                    : (ceval.getEvalRateBps() != null
                            ? ceval.getEvalRateBps()
                            : (product != null ? product.getBaseRateBps() : null));
            approvedPeriod = req.approvedPeriodMo() != null
                    ? req.approvedPeriodMo()
                    : application.getRequestedPeriodMo();
            approvedAt = now;
        }

        LoanReview saved = repository.save(LoanReview.builder()
                .applId(applId)
                .revTypeCd(req.revTypeCd())
                .revStatusCd(LoanReview.STATUS_COMPLETED)
                .revDecisionCd(req.revDecisionCd())
                .approvedAmount(approvedAmount)
                .approvedRateBps(approvedRate)
                .approvedPeriodMo(approvedPeriod)
                .rejectReasonCd(approved ? null : req.rejectReasonCd())
                .revRemark(req.revRemark())
                .reviewerId(req.reviewerId())
                .reviewedAt(now)
                .approvedAt(approvedAt)
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, saved.getRevId(),
                null, LoanReview.STATUS_COMPLETED,
                approved ? REASON_REVIEW_APPROVED : REASON_REVIEW_REJECTED,
                approved
                        ? "approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                        : "rejectReasonCd=" + req.rejectReasonCd(),
                actorId
        ));

        String applBefore = application.currentStatus();
        if (approved) {
            application.markApproved();
        } else {
            application.markRejected();
        }
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_APPLICATION, applId,
                applBefore, application.currentStatus(),
                approved ? REASON_REVIEW_APPROVED : REASON_REVIEW_REJECTED,
                "revId=" + saved.getRevId(),
                actorId
        ));

        return LoanReviewResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public LoanReviewResponse get(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));
        return repository.findByApplIdAndDeletedAtIsNull(applId)
                .map(LoanReviewResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));
    }

    /**
     * 승인 한도 자동 산정 — 신청금액 / CB 한도 / 상품 max 중 최솟값.
     */
    private long determineApprovedAmount(LoanApplication application,
                                         CreditEvaluation ceval,
                                         LoanProduct product) {
        long candidate = application.getRequestedAmount();
        if (ceval.getEvalLimitAmount() != null) {
            candidate = Math.min(candidate, ceval.getEvalLimitAmount());
        }
        if (product != null && product.getMaxAmount() != null) {
            candidate = Math.min(candidate, product.getMaxAmount());
        }
        return candidate;
    }
}
