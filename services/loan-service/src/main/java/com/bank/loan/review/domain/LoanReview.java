package com.bank.loan.review.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 본심사(Underwriting). ERD LOAN_REVIEW 매핑. appl_id UNIQUE — 신청당 1건.
 *
 * 사전조건: 가심사 PASS + CB(APPROVE/REVIEW) + DSR PASS.
 * 결정(rev_decision_cd):
 *   APPROVED  승인 — approved_amount/rate/period 확정, 신청 APPROVED 로 전이
 *   REJECTED  거절 — 신청 REJECTED 로 전이
 *
 * 심사 유형(rev_type_cd):
 *   AUTO    자동심사 (CB·DSR 통과 기준 자동 승인)
 *   MANUAL  수동심사 (심사관 결정)
 */
@Getter
@Entity
@Table(name = "loan_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanReview extends BaseEntity {

    public static final String TYPE_AUTO   = "AUTO";
    public static final String TYPE_MANUAL = "MANUAL";

    public static final String DECISION_APPROVED = "APPROVED";
    public static final String DECISION_REJECTED = "REJECTED";

    public static final String APPROVER_AS_IS            = "APPROVE_AS_IS";
    public static final String APPROVER_OVERRIDE_APPROVED = "OVERRIDE_APPROVED";
    public static final String APPROVER_OVERRIDE_REJECTED = "OVERRIDE_REJECTED";

    public static final String STATUS_PENDING_APPROVAL  = "PENDING_APPROVAL";
    public static final String STATUS_REVIEWER_DECIDED  = "REVIEWER_DECIDED";
    public static final String STATUS_BIAS_REVIEWING    = "BIAS_REVIEWING";
    public static final String STATUS_PENDING_APPROVER  = "PENDING_APPROVER";
    public static final String STATUS_COMPLETED         = "COMPLETED";
    public static final String STATUS_EXPIRED           = "EXPIRED";
    public static final String STATUS_ESCALATED_TO_HQ   = "ESCALATED_TO_HQ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rev_id")
    private Long revId;

    @Column(name = "appl_id", nullable = false, unique = true)
    private Long applId;

    @Column(name = "rev_type_cd", nullable = false, length = 50)
    private String revTypeCd;

    @Column(name = "rev_status_cd", nullable = false, length = 50)
    private String revStatusCd;

    @Column(name = "rev_decision_cd", length = 50)
    private String revDecisionCd;

    @Column(name = "approved_amount")
    private Long approvedAmount;

    @Column(name = "approved_rate_bps")
    private Integer approvedRateBps;

    @Column(name = "approved_period_mo")
    private Integer approvedPeriodMo;

    @Column(name = "reject_reason_cd", length = 50)
    private String rejectReasonCd;

    @Column(name = "rev_remark", length = 500)
    private String revRemark;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approved_decision_cd", length = 50)
    private String approvedDecisionCd;

    @Column(name = "override_reason_cd", length = 50)
    private String overrideReasonCd;

    @Column(name = "override_remark", length = 500)
    private String overrideRemark;

    @Column(name = "bias_severity_cd", length = 20)
    private String biasSeverityCd;

    @Column(name = "bias_override_by")
    private Long biasOverrideBy;

    @Column(name = "bias_override_reason", length = 500)
    private String biasOverrideReason;

    @Column(name = "bias_overridden_at")
    private OffsetDateTime biasOverriddenAt;

    @Column(name = "pending_approver_since")
    private OffsetDateTime pendingApproverSince;

    @Column(name = "rev_ai_track_cd", length = 20)
    private String revAiTrackCd;

    @Column(name = "rev_ai_pd", precision = 10, scale = 6)
    private BigDecimal revAiPd;

    @Column(name = "rev_ai_rationale", columnDefinition = "TEXT")
    private String revAiRationale;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    public boolean isApproved() {
        return DECISION_APPROVED.equals(revDecisionCd);
    }

    public boolean isPendingApproval() {
        return STATUS_PENDING_APPROVAL.equals(revStatusCd);
    }

    public boolean isBiasReviewing() {
        return STATUS_BIAS_REVIEWING.equals(revStatusCd);
    }

    public boolean isPendingApprover() {
        return STATUS_PENDING_APPROVER.equals(revStatusCd);
    }

    public boolean isBiasBlocked() {
        return AiReviewAdvice.SEVERITY_BLOCKED.equals(biasSeverityCd)
                && biasOverrideBy == null;
    }

    /**
     * REVIEWER_DECIDED 상태에서 편향 검증 단계로 진입. revStatusCd → BIAS_REVIEWING.
     * markReviewerDecided() 또는 confirm() 호출 직후 이어서 호출한다.
     * 신청 상태 전이는 여기서 하지 않는다.
     */
    public void markBiasReviewing() {
        this.revStatusCd = STATUS_BIAS_REVIEWING;
    }

    /**
     * 편향 검증 결과 캐시 갱신. 에이전트가 bias-report 를 적재한 뒤 호출.
     */
    public void updateBiasSeverity(String severityCd) {
        this.biasSeverityCd = severityCd;
    }

    /**
     * 상급자 BLOCKED 우회 승인. bias_override_* 기록.
     * BIAS_REVIEWING 상태 + biasSeverityCd = BLOCKED 일 때만 호출.
     */
    public void biasOverride(Long overrideBy, String reason, OffsetDateTime overriddenAt) {
        this.biasOverrideBy = overrideBy;
        this.biasOverrideReason = reason;
        this.biasOverriddenAt = overriddenAt;
    }

    /**
     * 심사원이 편향 리포트를 확인(acknowledge). revStatusCd → PENDING_APPROVER.
     * 호출 전 isBiasBlocked() = false 임을 검증해야 한다.
     * pendingApproverSince 는 expire-pending-approver 배치의 타임아웃 기준으로 사용된다.
     */
    public void acknowledgeBias() {
        this.revStatusCd = STATUS_PENDING_APPROVER;
        this.pendingApproverSince = OffsetDateTime.now();
    }

    /**
     * 승인자 최종 확정. revStatusCd → COMPLETED.
     *
     * approverDecisionCd:
     *   APPROVE_AS_IS      심사원 결정 그대로 확정
     *   OVERRIDE_APPROVED  결정을 APPROVED 로 변경 — approvedAmount/rate/period 필수
     *   OVERRIDE_REJECTED  결정을 REJECTED 로 변경 — rejectReasonCd 필수
     *
     * revDecisionCd 는 override 시에만 갱신. approvedAt 은 최종 APPROVED 일 때만 채운다.
     */
    public void approverApprove(Long approverId,
                                String approverDecisionCd,
                                String overrideReasonCd,
                                String overrideRemark,
                                Long overrideAmount,
                                Integer overrideRateBps,
                                Integer overridePeriodMo,
                                String overrideRejectReasonCd,
                                OffsetDateTime decidedAt) {
        this.revStatusCd = STATUS_COMPLETED;
        this.approverId = approverId;
        this.approvedDecisionCd = approverDecisionCd;
        this.overrideReasonCd = overrideReasonCd;
        this.overrideRemark = overrideRemark;

        if (APPROVER_OVERRIDE_APPROVED.equals(approverDecisionCd)) {
            this.revDecisionCd = DECISION_APPROVED;
            this.approvedAmount = overrideAmount;
            this.approvedRateBps = overrideRateBps;
            this.approvedPeriodMo = overridePeriodMo;
            this.rejectReasonCd = null;
        } else if (APPROVER_OVERRIDE_REJECTED.equals(approverDecisionCd)) {
            this.revDecisionCd = DECISION_REJECTED;
            this.approvedAmount = null;
            this.approvedRateBps = null;
            this.approvedPeriodMo = null;
            this.rejectReasonCd = overrideRejectReasonCd;
        }
        // APPROVE_AS_IS: revDecisionCd 그대로

        this.approvedAt = DECISION_APPROVED.equals(this.revDecisionCd) ? decidedAt : null;
    }

    public void markCompleted() {
        this.revStatusCd = STATUS_COMPLETED;
    }

    /**
     * BIAS_REVIEWING 상태의 본심사가 기한 내 진행되지 않아 만료.
     */
    public void expireBiasReviewing() {
        this.revStatusCd = STATUS_EXPIRED;
    }

    /**
     * PENDING_APPROVER 상태의 본심사가 기한 내 승인자 확정 없이 만료.
     */
    public void expirePendingApprover() {
        this.revStatusCd = STATUS_EXPIRED;
    }

    /**
     * 자동 권고(PENDING_APPROVAL) 결과를 사람이 확정. revStatusCd → REVIEWER_DECIDED,
     * reviewerId/reviewedAt 갱신. 신청 상태 전이와 approvedAt 은 승인자 단계로 이동.
     * 호출 후 markBiasReviewing() 을 이어 호출해 편향 검증 단계로 진입한다.
     */
    public void confirm(Long reviewerId, OffsetDateTime confirmedAt) {
        this.revStatusCd = STATUS_REVIEWER_DECIDED;
        this.reviewerId = reviewerId;
        this.reviewedAt = confirmedAt;
    }

    /**
     * 심사원 결정 완료 후 편향 검증 단계로 진입. revStatusCd → REVIEWER_DECIDED.
     * run() / confirm() 완료 시점에 호출. 신청 상태 전이는 여기서 하지 않는다.
     */
    public void markReviewerDecided() {
        this.revStatusCd = STATUS_REVIEWER_DECIDED;
    }

    /**
     * 자동 권고가 일정 기간 미확정 시 만료 처리. revStatusCd → EXPIRED.
     * 만료 시각은 status_history 가 보유한다 — 별도 컬럼 없음.
     */
    public void expire() {
        this.revStatusCd = STATUS_EXPIRED;
    }

    public boolean isEscalated() {
        return escalatedAt != null;
    }

    /** 이상거래 본사 상신. escalated_at 기록 + 상태 전이. */
    public void escalateToHq(OffsetDateTime at) {
        this.escalatedAt = at;
        this.revStatusCd = STATUS_ESCALATED_TO_HQ;
    }

    /**
     * 본심사 결정 정정(재심사). 결정·한도·금리·기간·거절사유·메모·심사관·시각을 일괄 갱신한다.
     * revStatusCd 는 COMPLETED 유지. 변경 이력은 STATUS_HISTORY 와 REVIEW_CHECK_LOG 가 담는다.
     */
    public void revise(String revDecisionCd,
                       Long approvedAmount,
                       Integer approvedRateBps,
                       Integer approvedPeriodMo,
                       String rejectReasonCd,
                       String revRemark,
                       Long reviewerId,
                       OffsetDateTime reviewedAt) {
        this.revDecisionCd = revDecisionCd;
        this.approvedAmount = approvedAmount;
        this.approvedRateBps = approvedRateBps;
        this.approvedPeriodMo = approvedPeriodMo;
        this.rejectReasonCd = rejectReasonCd;
        this.revRemark = revRemark;
        this.reviewerId = reviewerId;
        this.reviewedAt = reviewedAt;
        this.approvedAt = DECISION_APPROVED.equals(revDecisionCd) ? reviewedAt : null;
    }
}
