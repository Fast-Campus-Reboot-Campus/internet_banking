package com.bank.customer.party.domain;

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

/**
 * 관계자관계 (party_relation 테이블).
 * N:M self-reference. 예: 법인과 대표이사, 개인과 법인(주주) 등.
 */
@Entity
@Table(name = "party_relation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PartyRelation extends BaseEntity {

    /** 관계 유형 코드 */
    public static final String TYPE_REPRESENTATIVE = "REPRESENTATIVE"; // 대표이사
    public static final String TYPE_SHAREHOLDER    = "SHAREHOLDER";    // 주주
    public static final String TYPE_SPOUSE         = "SPOUSE";         // 배우자
    public static final String TYPE_PARENT         = "PARENT";         // 부모
    public static final String TYPE_GUARANTOR      = "GUARANTOR";      // 보증인

    /** 검토 상태 코드 (대리인 위임장 검토 큐) */
    public static final String REVIEW_PENDING  = "PENDING";
    public static final String REVIEW_APPROVED = "APPROVED";
    public static final String REVIEW_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_id")
    private Long relationId;

    @Column(name = "from_party_id", nullable = false)
    private Long fromPartyId;

    @Column(name = "to_party_id", nullable = false)
    private Long toPartyId;

    @Column(name = "relation_type_code", nullable = false, length = 10)
    private String relationTypeCode;

    @Column(name = "relation_detail_code", length = 10)
    private String relationDetailCode;

    /** 지분율 bps (1% = 100). 주주 관계에서 사용 */
    @Column(name = "equity_ratio_bps")
    private Integer equityRatioBps;

    @Column(name = "representation_scope", length = 200)
    private String representationScope;

    @Column(name = "proof_url", length = 500)
    private String proofUrl;

    /** YYYYMMDD */
    @Column(name = "relation_start_date", nullable = false, length = 8)
    private String relationStartDate;

    @Column(name = "relation_end_date", length = 8)
    private String relationEndDate;

    @Column(name = "relation_end_reason_code", length = 20)
    private String relationEndReasonCode;

    /** 대리인 위임장 검토 상태. NULL이면 검토 대상 아님(시드·자동 관계). */
    @Column(name = "relation_review_status_code", length = 20)
    private String relationReviewStatusCode;

    public boolean isActive() { return relationEndDate == null; }

    public boolean isReviewPending() { return REVIEW_PENDING.equals(relationReviewStatusCode); }

    public void end(String endDate, String reasonCode) {
        this.relationEndDate       = endDate;
        this.relationEndReasonCode = reasonCode;
    }

    /** 위임장 검토 승인. */
    public void approveReview() {
        this.relationReviewStatusCode = REVIEW_APPROVED;
    }

    /** 위임장 검토 거절(위조 의심 등). */
    public void rejectReview() {
        this.relationReviewStatusCode = REVIEW_REJECTED;
    }
}
