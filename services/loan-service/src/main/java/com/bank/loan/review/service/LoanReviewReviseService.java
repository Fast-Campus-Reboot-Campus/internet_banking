package com.bank.loan.review.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.ReviseReviewRequest;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 본심사 정정(재심사) — 신청 APPROVED/REJECTED 상태에서만 가능, 약정 진입(CONTRACTED) 후엔 LOAN_044.
 * 같은 LoanReview row 를 갱신하고 status_history + ReviewCheckLog 재적재로 이력 보존.
 */
@Service
@RequiredArgsConstructor
public class LoanReviewReviseService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_REVIEW = "LOAN_REVIEW";
    private static final String TARGET_APPLICATION = "LOAN_APPLICATION";
    private static final String REASON_REVISITED_APPROVED = "REVIEW_REVISITED_APPROVED";
    private static final String REASON_REVISITED_REJECTED = "REVIEW_REVISITED_REJECTED";

    private final LoanReviewRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final ApprovedAmountCalculator approvedAmountCalculator;
    private final LoanReviewCheckLogWriter checkLogWriter;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public LoanReviewResponse revise(Long applId, ReviseReviewRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanReview review = repository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        String applBefore = application.currentStatus();
        if (!LoanApplication.STATUS_APPROVED.equals(applBefore)
                && !LoanApplication.STATUS_REJECTED.equals(applBefore)) {
            throw new BusinessException(LoanErrorCode.LOAN_044, "current=" + applBefore);
        }

        boolean approved = LoanReview.DECISION_APPROVED.equals(req.revDecisionCd());
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long approvedAmount = null;
        Integer approvedRate = null;
        Integer approvedPeriod = null;
        CreditEvaluation ceval = null;
        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        if (approved) {
            ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId).orElse(null);
            approvedAmount = req.approvedAmount() != null
                    ? req.approvedAmount()
                    : approvedAmountCalculator.determine(application, ceval, product);
            approvedRate = req.approvedRateBps() != null
                    ? req.approvedRateBps()
                    : (ceval != null && ceval.getEvalRateBps() != null
                            ? ceval.getEvalRateBps()
                            : (product != null ? product.getBaseRateBps() : null));
            approvedPeriod = req.approvedPeriodMo() != null
                    ? req.approvedPeriodMo()
                    : application.getRequestedPeriodMo();
        }

        review.revise(
                req.revDecisionCd(),
                approvedAmount, approvedRate, approvedPeriod,
                approved ? null : req.rejectReasonCd(),
                req.revRemark(),
                req.reviewerId(),
                now
        );

        checkLogWriter.logRevisit(review.getRevId(), approved, req);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, review.getRevId(),
                LoanReview.STATUS_COMPLETED, LoanReview.STATUS_COMPLETED,
                approved ? REASON_REVISITED_APPROVED : REASON_REVISITED_REJECTED,
                "revisitReasonCd=" + req.revisitReasonCd()
                        + (approved
                                ? ", approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                                : ", rejectReasonCd=" + req.rejectReasonCd()),
                actorId
        ));

        String applAfter = approved ? LoanApplication.STATUS_APPROVED : LoanApplication.STATUS_REJECTED;
        if (!applBefore.equals(applAfter)) {
            if (approved) {
                application.markApproved();
            } else {
                application.markRejected();
            }
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_APPLICATION, applId,
                    applBefore, applAfter,
                    approved ? REASON_REVISITED_APPROVED : REASON_REVISITED_REJECTED,
                    "revId=" + review.getRevId()
                            + ", revisitReasonCd=" + req.revisitReasonCd(),
                    actorId
            ));
        }

        return LoanReviewResponse.of(review);
    }
}
