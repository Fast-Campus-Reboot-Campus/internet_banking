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

    public static final String STATUS_COMPLETED = "COMPLETED";

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
}
