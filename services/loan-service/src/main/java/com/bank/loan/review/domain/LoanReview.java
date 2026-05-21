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

    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_COMPLETED         = "COMPLETED";
    public static final String STATUS_EXPIRED           = "EXPIRED";

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

    public boolean isApproved() {
        return DECISION_APPROVED.equals(revDecisionCd);
    }

    public boolean isPendingApproval() {
        return STATUS_PENDING_APPROVAL.equals(revStatusCd);
    }

    /**
     * 자동 권고(PENDING_APPROVAL) 결과를 사람이 확정. revStatusCd → COMPLETED,
     * reviewerId/reviewedAt 갱신. APPROVED 권고일 때만 approvedAt 도 같이 채운다.
     */
    public void confirm(Long reviewerId, OffsetDateTime confirmedAt) {
        this.revStatusCd = STATUS_COMPLETED;
        this.reviewerId = reviewerId;
        this.reviewedAt = confirmedAt;
        if (DECISION_APPROVED.equals(revDecisionCd) && approvedAt == null) {
            this.approvedAt = confirmedAt;
        }
    }

    /**
     * 자동 권고가 일정 기간 미확정 시 만료 처리. revStatusCd → EXPIRED.
     * 만료 시각은 status_history 가 보유한다 — 별도 컬럼 없음.
     */
    public void expire() {
        this.revStatusCd = STATUS_EXPIRED;
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
