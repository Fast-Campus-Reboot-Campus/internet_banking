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
import com.bank.loan.ltv.domain.LtvCalculation;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.notification.channel.KafkaChannelAdapter;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import com.bank.loan.review.dto.ConfirmReviewRequest;
import com.bank.loan.review.dto.ExpirePendingReviewsResponse;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.event.LoanBiasCheckRequestedPayload;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 자동 본심사(권고) 경로 — autoDecide → confirm 또는 expirePending.
 * 결정 룰: CB.REJECT → REJECTED, DSR.FAIL → REJECTED, LTV.FAIL → REJECTED, 그 외 → APPROVED.
 * 권고는 PENDING_APPROVAL 로만 저장되며 confirm 시점에 신청 상태 전이.
 */
@Service
@RequiredArgsConstructor
public class LoanReviewAutoDecideService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_REVIEW = "LOAN_REVIEW";
    private static final String TARGET_APPLICATION = "LOAN_APPLICATION";
    private static final String REASON_AUTO_RECOMMENDED_APPROVED = "AUTO_RECOMMENDED_APPROVED";
    private static final String REASON_AUTO_RECOMMENDED_REJECTED = "AUTO_RECOMMENDED_REJECTED";
    private static final String REASON_REVIEW_APPROVED = "REVIEW_APPROVED";
    private static final String REASON_REVIEW_REJECTED = "REVIEW_REJECTED";
    private static final String REASON_REVIEW_CONFIRMED    = "REVIEW_CONFIRMED";
    private static final String REASON_REVIEW_EXPIRED      = "AUTO_RECOMMENDATION_EXPIRED";
    private static final String REASON_BIAS_CHECK_TRIGGERED = "BIAS_CHECK_TRIGGERED";

    private static final String REJECT_CB  = "CB_REJECT";
    private static final String REJECT_DSR = "DSR_OVER";
    private static final String REJECT_LTV = "LTV_FAIL";

    private final LoanReviewRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final DsrCalculationRepository dsrCalculationRepository;
    private final LoanReviewPreconditions preconditions;
    private final ApprovedAmountCalculator approvedAmountCalculator;
    private final LoanReviewCheckLogWriter checkLogWriter;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final NotificationOutboxAppender outboxAppender;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<LoanReviewResponse> listPending() {
        return repository
                .findByRevStatusCdAndDeletedAtIsNullOrderByReviewedAtAsc(LoanReview.STATUS_PENDING_APPROVAL)
                .stream()
                .map(LoanReviewResponse::of)
                .toList();
    }

    @Transactional
    public LoanReviewResponse autoDecide(Long applId) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_039);
        }
        if (!application.isReviewable()) {
            throw new BusinessException(LoanErrorCode.LOAN_038, "current=" + application.currentStatus());
        }

        CreditEvaluation ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038, "credit-evaluation required"));
        if (CreditEvaluation.DECISION_REVIEW.equals(ceval.getCevalDecisionCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_048, "cevalDecision=REVIEW");
        }

        DsrCalculation dsr = dsrCalculationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038, "dsr-calculation required"));

        preconditions.requireIdvPass(applId);

        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        boolean collateralRequired = product != null && product.isCollateralRequired();
        LtvCalculation chosenLtv = null;
        if (collateralRequired) {
            chosenLtv = preconditions.resolveActiveLtvForAuto(applId);
        }

        String decision;
        String rejectReasonCd = null;
        if (CreditEvaluation.DECISION_REJECT.equals(ceval.getCevalDecisionCd())) {
            decision = LoanReview.DECISION_REJECTED;
            rejectReasonCd = REJECT_CB;
        } else if (!DsrCalculation.STATUS_PASS.equals(dsr.getDsrStatusCd())) {
            decision = LoanReview.DECISION_REJECTED;
            rejectReasonCd = REJECT_DSR;
        } else if (collateralRequired && !LtvCalculation.STATUS_PASS.equals(chosenLtv.getLtvStatusCd())) {
            decision = LoanReview.DECISION_REJECTED;
            rejectReasonCd = REJECT_LTV;
        } else {
            decision = LoanReview.DECISION_APPROVED;
        }

        boolean approved = LoanReview.DECISION_APPROVED.equals(decision);
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long approvedAmount = null;
        Integer approvedRate = null;
        Integer approvedPeriod = null;
        if (approved) {
            approvedAmount = approvedAmountCalculator.determine(application, ceval, product);
            approvedRate = ceval.getEvalRateBps() != null
                    ? ceval.getEvalRateBps()
                    : (product != null ? product.getBaseRateBps() : null);
            approvedPeriod = application.getRequestedPeriodMo();
        }

        LoanReview saved = repository.save(LoanReview.builder()
                .applId(applId)
                .revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_PENDING_APPROVAL)
                .revDecisionCd(decision)
                .approvedAmount(approvedAmount)
                .approvedRateBps(approvedRate)
                .approvedPeriodMo(approvedPeriod)
                .rejectReasonCd(rejectReasonCd)
                .revRemark(null)
                .reviewerId(null)
                .reviewedAt(now)
                .approvedAt(null)
                .build());

        checkLogWriter.logAuto(saved.getRevId(), ceval, dsr, chosenLtv, collateralRequired, approved, rejectReasonCd);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, saved.getRevId(),
                null, LoanReview.STATUS_PENDING_APPROVAL,
                approved ? REASON_AUTO_RECOMMENDED_APPROVED : REASON_AUTO_RECOMMENDED_REJECTED,
                approved
                        ? "auto, approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                        : "auto, rejectReasonCd=" + rejectReasonCd,
                actorId
        ));

        return LoanReviewResponse.of(saved);
    }

    @Transactional
    public LoanReviewResponse confirm(Long applId, ConfirmReviewRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanReview review = repository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        if (!review.isPendingApproval()) {
            throw new BusinessException(LoanErrorCode.LOAN_049, "revStatus=" + review.getRevStatusCd());
        }

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();
        boolean approved = review.isApproved();

        // confirm() 이 REVIEWER_DECIDED 로 전이 (reviewerId/reviewedAt 갱신 포함)
        review.confirm(req.reviewerId(), now);

        checkLogWriter.logConfirm(review.getRevId(), approved, req.reviewerId(), req.confirmRemark());

        // 심사원 확정 이력
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, review.getRevId(),
                LoanReview.STATUS_PENDING_APPROVAL, LoanReview.STATUS_REVIEWER_DECIDED,
                REASON_REVIEW_CONFIRMED,
                "reviewerId=" + req.reviewerId(),
                actorId
        ));

        // 편향 검증 단계 진입
        review.markBiasReviewing();
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, review.getRevId(),
                LoanReview.STATUS_REVIEWER_DECIDED, LoanReview.STATUS_BIAS_REVIEWING,
                REASON_BIAS_CHECK_TRIGGERED, null, actorId
        ));

        enqueueBiasCheck(review, application);
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
                "confirm, revId=" + review.getRevId(),
                actorId
        ));

        return LoanReviewResponse.of(review);
    }

    private void enqueueBiasCheck(LoanReview review, LoanApplication application) {
        Long applId = application.getApplId();
        CreditEvaluation ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new IllegalStateException("bias-check: ceval not found applId=" + applId));
        DsrCalculation dsr = dsrCalculationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new IllegalStateException("bias-check: dsr not found applId=" + applId));
        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        try {
            var payload = new LoanBiasCheckRequestedPayload(
                    LoanBiasCheckRequestedPayload.EVENT_TYPE_CD,
                    OffsetDateTime.now(),
                    review.getRevId(),
                    applId,
                    review.getRevTypeCd(),
                    new LoanBiasCheckRequestedPayload.ReviewerDecision(
                            review.getRevDecisionCd(),
                            review.getRejectReasonCd(),
                            review.getApprovedAmount(),
                            review.getApprovedRateBps(),
                            review.getApprovedPeriodMo(),
                            review.getReviewerId(),
                            review.getReviewedAt()
                    ),
                    new LoanBiasCheckRequestedPayload.ReviewContext(
                            product != null ? product.getProdCd() : null,
                            ceval.getCevalDecisionCd(),
                            ceval.getCevalScore(),
                            dsr.getDsrRatioBps(),
                            dsr.getDsrLimitBps(),
                            null
                    )
            );
            outboxAppender.enqueueInCurrentTx(
                    LoanBiasCheckRequestedPayload.EVENT_TYPE_CD,
                    review.getRevId(),
                    KafkaChannelAdapter.CHANNEL_CD,
                    objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("bias-check payload 직렬화 실패 revId=" + review.getRevId(), e);
        }
    }

    /**
     * 권고 만료 배치. cutoff(=now - olderThanDays) 이전 PENDING_APPROVAL → EXPIRED.
     * 신청 상태는 PRESCREENED 그대로 유지 — 운영자가 별도 처리(수동 본심사 등) 필요.
     */
    @Transactional
    public ExpirePendingReviewsResponse expirePending(int olderThanDays) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(olderThanDays);
        Long actorId = currentActor.currentActorId();

        List<LoanReview> targets = repository
                .findByRevStatusCdAndReviewedAtBeforeAndDeletedAtIsNull(
                        LoanReview.STATUS_PENDING_APPROVAL, cutoff);

        List<Long> expiredRevIds = new ArrayList<>();
        for (LoanReview rev : targets) {
            rev.expire();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_REVIEW, rev.getRevId(),
                    LoanReview.STATUS_PENDING_APPROVAL, LoanReview.STATUS_EXPIRED,
                    REASON_REVIEW_EXPIRED,
                    "cutoffAt=" + cutoff + ", olderThanDays=" + olderThanDays,
                    actorId
            ));
            expiredRevIds.add(rev.getRevId());
        }
        return new ExpirePendingReviewsResponse(targets.size(), expiredRevIds, cutoff);
    }
}
