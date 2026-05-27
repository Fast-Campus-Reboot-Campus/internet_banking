package com.bank.loan.review.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.review.domain.AiReviewAdvice;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.AiReviewAdviceResponse;
import com.bank.loan.review.dto.BiasOpsNoteRequest;
import com.bank.loan.review.dto.ExpireBiasReviewingResponse;
import com.bank.loan.review.dto.ExpirePendingApproverResponse;
import com.bank.loan.review.repository.AiReviewAdviceRepository;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanReviewOpsService {

    private static final String DOMAIN_CD       = "LOAN";
    private static final String TARGET_REVIEW   = "LOAN_REVIEW";
    private static final String REASON_OPS_NOTE              = "OPS_NOTE_ADDED";
    private static final String REASON_EXPIRED               = "BIAS_REVIEWING_EXPIRED";
    private static final String REASON_PENDING_APPROVER_EXPIRED = "PENDING_APPROVER_EXPIRED";

    private final LoanReviewRepository reviewRepository;
    private final AiReviewAdviceRepository adviceRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    /**
     * 운영자가 BIAS_REVIEWING 건에 운영 노트를 주입.
     * severity=NONE advice 를 적재하고 review.biasSeverityCd 를 NONE 으로 갱신.
     * 결과: isBiasBlocked() = false → 심사원이 acknowledge-bias 진행 가능.
     * 4-eye 우회 권한은 없음 — 승인자는 별도.
     */
    @Transactional
    public AiReviewAdviceResponse addBiasOpsNote(Long revId, BiasOpsNoteRequest req) {
        LoanReview review = reviewRepository.findById(revId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        if (!review.isBiasReviewing()) {
            throw new BusinessException(LoanErrorCode.LOAN_192);
        }

        AiReviewAdvice advice = adviceRepository.save(AiReviewAdvice.builder()
                .revId(revId)
                .adviceTypeCd(AiReviewAdvice.TYPE_BIAS_CHECK)
                .severityCd(AiReviewAdvice.SEVERITY_NONE)
                .adviceBody(req.note())
                .model("OPS")
                .modelVersion("manual")
                .build());

        review.updateBiasSeverity(AiReviewAdvice.SEVERITY_NONE);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, revId,
                LoanReview.STATUS_BIAS_REVIEWING, LoanReview.STATUS_BIAS_REVIEWING,
                REASON_OPS_NOTE,
                "opsStaffId=" + req.opsStaffId(),
                currentActor.currentActorId()
        ));

        return AiReviewAdviceResponse.of(advice);
    }

    /**
     * 일정 기간 이상 BIAS_REVIEWING 상태에서 진행되지 않은 건을 일괄 만료.
     * 만료된 건의 신청 상태는 PRESCREENED 유지 — 별도 재심사로 처리.
     */
    @Transactional
    public ExpireBiasReviewingResponse expireBiasReviewing(int olderThanDays) {
        OffsetDateTime cutoffAt = OffsetDateTime.now().minusDays(olderThanDays);

        List<LoanReview> targets = reviewRepository
                .findByRevStatusCdAndReviewedAtBeforeAndDeletedAtIsNull(
                        LoanReview.STATUS_BIAS_REVIEWING, cutoffAt);

        List<Long> expiredIds = targets.stream().map(r -> {
            r.expireBiasReviewing();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_REVIEW, r.getRevId(),
                    LoanReview.STATUS_BIAS_REVIEWING, LoanReview.STATUS_EXPIRED,
                    REASON_EXPIRED,
                    "olderThanDays=" + olderThanDays,
                    currentActor.currentActorId()
            ));
            return r.getRevId();
        }).sorted().toList();

        return new ExpireBiasReviewingResponse(expiredIds.size(), expiredIds, cutoffAt);
    }

    /**
     * 일정 기간 이상 PENDING_APPROVER 상태에서 승인자 확정 없이 방치된 건을 일괄 만료.
     * pendingApproverSince 가 cutoffAt 이전인 건 + NULL(마이그레이션 이전 레거시 건) 모두 처리.
     * 만료된 건의 신청 상태는 PRESCREENED 유지 — 운영자가 별도 재심사로 처리.
     */
    @Transactional
    public ExpirePendingApproverResponse expirePendingApprover(int olderThanDays) {
        OffsetDateTime cutoffAt = OffsetDateTime.now().minusDays(olderThanDays);
        Long actorId = currentActor.currentActorId();

        List<LoanReview> targets = new java.util.ArrayList<>();
        targets.addAll(reviewRepository.findByRevStatusCdAndPendingApproverSinceBeforeAndDeletedAtIsNull(
                LoanReview.STATUS_PENDING_APPROVER, cutoffAt));
        // pendingApproverSince 가 없는 레거시 건도 포함 (V20 이전 데이터)
        targets.addAll(reviewRepository.findByRevStatusCdAndPendingApproverSinceIsNullAndDeletedAtIsNull(
                LoanReview.STATUS_PENDING_APPROVER));

        List<Long> expiredIds = targets.stream().map(r -> {
            r.expirePendingApprover();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_REVIEW, r.getRevId(),
                    LoanReview.STATUS_PENDING_APPROVER, LoanReview.STATUS_EXPIRED,
                    REASON_PENDING_APPROVER_EXPIRED,
                    "olderThanDays=" + olderThanDays,
                    actorId
            ));
            return r.getRevId();
        }).sorted().toList();

        return new ExpirePendingApproverResponse(expiredIds.size(), expiredIds, cutoffAt);
    }
}
