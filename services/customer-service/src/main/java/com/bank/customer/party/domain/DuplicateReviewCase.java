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
 * 중복고객 검토 케이스 (duplicate_review_case 테이블).
 * 신규 party와 기존 party를 중복 후보로 묶어 직원이 복본/별개를 판정한다.
 */
@Getter
@Entity
@Table(name = "duplicate_review_case")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DuplicateReviewCase extends BaseEntity {

    public static final String MATCH_CI         = "CI";         // 본인확인 CI 충돌
    public static final String MATCH_NAME_BIRTH = "NAME_BIRTH"; // 이름+생년월일 일치

    public static final String STATUS_PENDING   = "PENDING";    // 검토대기
    public static final String STATUS_DUPLICATE = "DUPLICATE";  // 복본 확정
    public static final String STATUS_DISTINCT  = "DISTINCT";   // 별개(동명이인 등) 확정

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "duplicate_review_case_id")
    private Long duplicateReviewCaseId;

    @Column(name = "new_party_id", nullable = false)
    private Long newPartyId;

    @Column(name = "existing_party_id", nullable = false)
    private Long existingPartyId;

    @Column(name = "match_type_code", nullable = false, length = 20)
    private String matchTypeCode;

    @Column(name = "review_status_code", nullable = false, length = 20)
    private String reviewStatusCode;

    @Column(name = "reviewer_employee_id")
    private Long reviewerEmployeeId;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    public boolean isPending() { return STATUS_PENDING.equals(reviewStatusCode); }

    /** 복본 확정. */
    public void markDuplicate(Long reviewerEmployeeId, String comment) {
        transition(STATUS_DUPLICATE, reviewerEmployeeId, comment);
    }

    /** 별개(동명이인 등) 확정. */
    public void markDistinct(Long reviewerEmployeeId, String comment) {
        transition(STATUS_DISTINCT, reviewerEmployeeId, comment);
    }

    private void transition(String status, Long reviewerEmployeeId, String comment) {
        this.reviewStatusCode    = status;
        this.reviewerEmployeeId  = reviewerEmployeeId;
        this.reviewComment       = comment;
        this.reviewedAt          = OffsetDateTime.now();
    }
}
