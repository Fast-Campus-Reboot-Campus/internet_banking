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

import java.time.OffsetDateTime;

/**
 * 제재 스크리닝 Hit (sanction_screening_hit 테이블).
 * 스크리닝 탐지 건별 일치율·Hit유형·검토상태를 담는다. 제재대상 Hit 검토 화면의 검토 단위.
 */
@Getter
@Entity
@Table(name = "sanction_screening_hit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SanctionScreeningHit extends BaseEntity {

    public static final String STATUS_PENDING   = "PENDING";   // 검토대기
    public static final String STATUS_CLEARED   = "CLEARED";   // 동명이인 등 → 통과
    public static final String STATUS_CONFIRMED = "CONFIRMED"; // 제재 확정

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_screening_hit_id")
    private Long sanctionScreeningHitId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "hit_type_code", nullable = false, length = 30)
    private String hitTypeCode;

    @Column(name = "match_rate", nullable = false)
    private Integer matchRate;

    @Column(name = "screening_status_code", nullable = false, length = 20)
    private String screeningStatusCode;

    @Column(name = "reviewer_employee_id")
    private Long reviewerEmployeeId;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    public boolean isPending() { return STATUS_PENDING.equals(screeningStatusCode); }

    /** 동명이인 등으로 무혐의 처리(통과). */
    public void clearAsHomonym(Long reviewerEmployeeId, String comment) {
        this.screeningStatusCode = STATUS_CLEARED;
        this.reviewerEmployeeId  = reviewerEmployeeId;
        this.reviewComment       = comment;
        this.reviewedAt          = OffsetDateTime.now();
    }

    /** 제재 대상 확정. */
    public void confirmSanction(Long reviewerEmployeeId, String comment) {
        this.screeningStatusCode = STATUS_CONFIRMED;
        this.reviewerEmployeeId  = reviewerEmployeeId;
        this.reviewComment       = comment;
        this.reviewedAt          = OffsetDateTime.now();
    }
}
