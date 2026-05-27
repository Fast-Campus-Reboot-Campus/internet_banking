package com.bank.loan;

import com.bank.loan.review.domain.AiReviewAdvice;
import com.bank.loan.review.domain.LoanReview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoanReview 상태 전이 메서드 단위 테스트.
 * Spring 컨텍스트 불필요 — 순수 도메인 로직만 검증.
 */
class LoanReviewTransitionTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    // ------------------------------------------------------------------ markBiasReviewing

    @Test
    @DisplayName("markBiasReviewing — PENDING_APPROVAL → BIAS_REVIEWING")
    void markBiasReviewing_전이() {
        LoanReview review = pendingApprovalApproved();

        review.markBiasReviewing();

        assertThat(review.getRevStatusCd()).isEqualTo(LoanReview.STATUS_BIAS_REVIEWING);
        assertThat(review.isBiasReviewing()).isTrue();
    }

    // ------------------------------------------------------------------ confirm

    @Test
    @DisplayName("confirm — PENDING_APPROVAL → REVIEWER_DECIDED, reviewerId 기록, approvedAt 미기록")
    void confirm_전이() {
        LoanReview review = pendingApprovalApproved();

        review.confirm(201L, NOW);

        assertThat(review.getRevStatusCd()).isEqualTo(LoanReview.STATUS_REVIEWER_DECIDED);
        assertThat(review.getReviewerId()).isEqualTo(201L);
        assertThat(review.getReviewedAt()).isEqualTo(NOW);
        assertThat(review.getApprovedAt()).isNull();
    }

    // ------------------------------------------------------------------ updateBiasSeverity

    @Test
    @DisplayName("updateBiasSeverity — severity 캐시 갱신")
    void updateBiasSeverity() {
        LoanReview review = biasReviewingApproved();

        review.updateBiasSeverity(AiReviewAdvice.SEVERITY_MEDIUM);

        assertThat(review.getBiasSeverityCd()).isEqualTo(AiReviewAdvice.SEVERITY_MEDIUM);
        assertThat(review.isBiasBlocked()).isFalse();
    }

    @Test
    @DisplayName("isBiasBlocked — BLOCKED + override 없음 → true")
    void isBiasBlocked_true() {
        LoanReview review = biasReviewingApproved();
        review.updateBiasSeverity(AiReviewAdvice.SEVERITY_BLOCKED);

        assertThat(review.isBiasBlocked()).isTrue();
    }

    @Test
    @DisplayName("isBiasBlocked — BLOCKED 이지만 override 있음 → false")
    void isBiasBlocked_override_후_false() {
        LoanReview review = biasReviewingApproved();
        review.updateBiasSeverity(AiReviewAdvice.SEVERITY_BLOCKED);
        review.biasOverride(999L, "상급자 검토 완료", NOW);

        assertThat(review.isBiasBlocked()).isFalse();
    }

    // ------------------------------------------------------------------ biasOverride

    @Test
    @DisplayName("biasOverride — override 정보 기록")
    void biasOverride_기록() {
        LoanReview review = biasReviewingApproved();
        review.updateBiasSeverity(AiReviewAdvice.SEVERITY_BLOCKED);

        review.biasOverride(999L, "리스크 위원회 승인", NOW);

        assertThat(review.getBiasOverrideBy()).isEqualTo(999L);
        assertThat(review.getBiasOverrideReason()).isEqualTo("리스크 위원회 승인");
        assertThat(review.getBiasOverriddenAt()).isEqualTo(NOW);
    }

    // ------------------------------------------------------------------ acknowledgeBias

    @Test
    @DisplayName("acknowledgeBias — BIAS_REVIEWING → PENDING_APPROVER")
    void acknowledgeBias_전이() {
        LoanReview review = biasReviewingApproved();
        review.updateBiasSeverity(AiReviewAdvice.SEVERITY_LOW);

        review.acknowledgeBias();

        assertThat(review.getRevStatusCd()).isEqualTo(LoanReview.STATUS_PENDING_APPROVER);
        assertThat(review.isPendingApprover()).isTrue();
    }

    // ------------------------------------------------------------------ approverApprove

    @Test
    @DisplayName("approverApprove AS_IS (APPROVED) — COMPLETED, approvedAt 설정, revDecisionCd 유지")
    void approverApprove_asIs_approved() {
        LoanReview review = pendingApproverApproved();

        review.approverApprove(301L, LoanReview.APPROVER_AS_IS,
                null, null, null, null, null, null, NOW);

        assertThat(review.getRevStatusCd()).isEqualTo(LoanReview.STATUS_COMPLETED);
        assertThat(review.getApproverId()).isEqualTo(301L);
        assertThat(review.getApprovedDecisionCd()).isEqualTo(LoanReview.APPROVER_AS_IS);
        assertThat(review.getRevDecisionCd()).isEqualTo(LoanReview.DECISION_APPROVED);
        assertThat(review.getApprovedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("approverApprove AS_IS (REJECTED) — COMPLETED, approvedAt null")
    void approverApprove_asIs_rejected() {
        LoanReview review = pendingApproverRejected();

        review.approverApprove(301L, LoanReview.APPROVER_AS_IS,
                null, null, null, null, null, null, NOW);

        assertThat(review.getRevStatusCd()).isEqualTo(LoanReview.STATUS_COMPLETED);
        assertThat(review.getRevDecisionCd()).isEqualTo(LoanReview.DECISION_REJECTED);
        assertThat(review.getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("approverApprove OVERRIDE_APPROVED — revDecisionCd APPROVED 로 변경, 금액 갱신")
    void approverApprove_overrideApproved() {
        LoanReview review = pendingApproverRejected();

        review.approverApprove(301L, LoanReview.APPROVER_OVERRIDE_APPROVED,
                "RISK_TOLERANCE", "추가 검토 후 승인",
                40_000_000L, 480, 48, null, NOW);

        assertThat(review.getRevDecisionCd()).isEqualTo(LoanReview.DECISION_APPROVED);
        assertThat(review.getApprovedAmount()).isEqualTo(40_000_000L);
        assertThat(review.getApprovedRateBps()).isEqualTo(480);
        assertThat(review.getApprovedPeriodMo()).isEqualTo(48);
        assertThat(review.getRejectReasonCd()).isNull();
        assertThat(review.getApprovedAt()).isEqualTo(NOW);
        assertThat(review.getOverrideReasonCd()).isEqualTo("RISK_TOLERANCE");
    }

    @Test
    @DisplayName("approverApprove OVERRIDE_REJECTED — revDecisionCd REJECTED 로 변경, 금액 null")
    void approverApprove_overrideRejected() {
        LoanReview review = pendingApproverApproved();

        review.approverApprove(301L, LoanReview.APPROVER_OVERRIDE_REJECTED,
                "BIAS_FIX", "편향 위험으로 거절 전환",
                null, null, null, "DSR_OVER", NOW);

        assertThat(review.getRevDecisionCd()).isEqualTo(LoanReview.DECISION_REJECTED);
        assertThat(review.getApprovedAmount()).isNull();
        assertThat(review.getApprovedRateBps()).isNull();
        assertThat(review.getRejectReasonCd()).isEqualTo("DSR_OVER");
        assertThat(review.getApprovedAt()).isNull();
        assertThat(review.getOverrideReasonCd()).isEqualTo("BIAS_FIX");
    }

    // ------------------------------------------------------------------ expireBiasReviewing

    @Test
    @DisplayName("expireBiasReviewing — BIAS_REVIEWING → EXPIRED")
    void expireBiasReviewing_전이() {
        LoanReview review = biasReviewingApproved();

        review.expireBiasReviewing();

        assertThat(review.getRevStatusCd()).isEqualTo(LoanReview.STATUS_EXPIRED);
    }

    // ------------------------------------------------------------------ fixtures

    private LoanReview pendingApprovalApproved() {
        return LoanReview.builder()
                .applId(1L)
                .revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_PENDING_APPROVAL)
                .revDecisionCd(LoanReview.DECISION_APPROVED)
                .approvedAmount(50_000_000L)
                .approvedRateBps(500)
                .approvedPeriodMo(60)
                .build();
    }

    private LoanReview biasReviewingApproved() {
        return LoanReview.builder()
                .applId(2L)
                .revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_BIAS_REVIEWING)
                .revDecisionCd(LoanReview.DECISION_APPROVED)
                .approvedAmount(50_000_000L)
                .approvedRateBps(500)
                .approvedPeriodMo(60)
                .reviewerId(201L)
                .reviewedAt(NOW)
                .build();
    }

    private LoanReview pendingApproverApproved() {
        return LoanReview.builder()
                .applId(3L)
                .revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_PENDING_APPROVER)
                .revDecisionCd(LoanReview.DECISION_APPROVED)
                .approvedAmount(50_000_000L)
                .approvedRateBps(500)
                .approvedPeriodMo(60)
                .reviewerId(201L)
                .reviewedAt(NOW)
                .build();
    }

    private LoanReview pendingApproverRejected() {
        return LoanReview.builder()
                .applId(4L)
                .revTypeCd(LoanReview.TYPE_MANUAL)
                .revStatusCd(LoanReview.STATUS_PENDING_APPROVER)
                .revDecisionCd(LoanReview.DECISION_REJECTED)
                .rejectReasonCd("CB_REJECT")
                .reviewerId(201L)
                .reviewedAt(NOW)
                .build();
    }
}
