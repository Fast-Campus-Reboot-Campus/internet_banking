package com.bank.loan.review.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.review.domain.AiReviewAdvice;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.AcknowledgeBiasRequest;
import com.bank.loan.review.dto.BiasOverrideRequest;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.repository.AiReviewAdviceRepository;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LoanReviewAcknowledgeBiasService {

    private static final String DOMAIN_CD        = "LOAN";
    private static final String TARGET_REVIEW    = "LOAN_REVIEW";
    private static final String REASON_BIAS_ACK  = "BIAS_ACKNOWLEDGED";
    private static final String REASON_BIAS_OVR  = "BIAS_OVERRIDE";

    private final LoanReviewRepository reviewRepository;
    private final LoanApplicationRepository applicationRepository;
    private final AiReviewAdviceRepository adviceRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    /**
     * 심사원이 편향 리포트를 확인 후 승인자 단계로 넘긴다.
     * BIAS_REVIEWING 상태 + 리포트 존재 + !BLOCKED 검증.
     * revStatusCd: BIAS_REVIEWING → PENDING_APPROVER
     */
    @Transactional
    public LoanReviewResponse acknowledgeBias(Long applId, AcknowledgeBiasRequest req) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanReview review = reviewRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        if (!review.isBiasReviewing()) {
            throw new BusinessException(LoanErrorCode.LOAN_192);
        }

        boolean reportExists = adviceRepository
                .findFirstByRevIdAndAdviceTypeCdOrderByCreatedAtDesc(
                        review.getRevId(), AiReviewAdvice.TYPE_BIAS_CHECK)
                .isPresent();
        if (!reportExists) {
            throw new BusinessException(LoanErrorCode.LOAN_193);
        }

        if (review.isBiasBlocked()) {
            throw new BusinessException(LoanErrorCode.LOAN_194);
        }

        review.acknowledgeBias();

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, review.getRevId(),
                LoanReview.STATUS_BIAS_REVIEWING, LoanReview.STATUS_PENDING_APPROVER,
                REASON_BIAS_ACK,
                req != null && req.acknowledgeRemark() != null ? req.acknowledgeRemark() : null,
                currentActor.currentActorId()
        ));

        return LoanReviewResponse.of(review);
    }

    /**
     * 상급자가 BLOCKED 편향 결과를 우회 승인.
     * BIAS_REVIEWING 상태일 때만 허용 — 심사원이 이후 acknowledgeBias() 로 진행 가능.
     * 4-eye 원칙: 해당 본심사를 수행한 심사원 본인은 우회 승인 불가.
     */
    @Transactional
    public LoanReviewResponse biasOverride(Long revId, BiasOverrideRequest req) {
        LoanReview review = reviewRepository.findById(revId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        if (!review.isBiasReviewing()) {
            throw new BusinessException(LoanErrorCode.LOAN_199);
        }

        // 4-eye: 심사원 본인이 자신의 편향 우회 승인 금지
        Long actorId = currentActor.currentActorId();
        if (review.getReviewerId() != null
                && !CurrentActorProvider.SYSTEM.equals(actorId)
                && review.getReviewerId().equals(actorId)) {
            throw new BusinessException(LoanErrorCode.LOAN_200);
        }

        // 우회 승인자는 인증 토큰(currentActorId)으로만 식별한다. 요청 바디 값은 받지 않는다.
        review.biasOverride(actorId, req.overrideReason(), OffsetDateTime.now());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, revId,
                LoanReview.STATUS_BIAS_REVIEWING, LoanReview.STATUS_BIAS_REVIEWING,
                REASON_BIAS_OVR,
                "overrideBy=" + actorId,
                actorId
        ));

        return LoanReviewResponse.of(review);
    }
}
