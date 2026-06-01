package com.bank.loan.review.repository;

import com.bank.loan.review.domain.LoanReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanReviewRepository extends JpaRepository<LoanReview, Long> {

    Optional<LoanReview> findByApplIdAndDeletedAtIsNull(Long applId);

    List<LoanReview> findByRevStatusCdAndDeletedAtIsNullOrderByReviewedAtAsc(String revStatusCd);

    /** 백필용 — 완료 심사를 revId 오름차순 페이지 단위로 조회. */
    Page<LoanReview> findByRevStatusCdAndDeletedAtIsNull(String revStatusCd, Pageable pageable);

    /** 백필 날짜 범위 — from ≤ reviewedAt < to, 완료 건만. */
    Page<LoanReview> findByRevStatusCdAndReviewedAtGreaterThanEqualAndReviewedAtLessThanAndDeletedAtIsNull(
            String revStatusCd, OffsetDateTime fromInclusive, OffsetDateTime toExclusive, Pageable pageable);

    List<LoanReview> findByRevStatusCdAndReviewedAtBeforeAndDeletedAtIsNull(
            String revStatusCd, OffsetDateTime cutoffAt);

    /**
     * PENDING_APPROVER 타임아웃 배치용.
     * pendingApproverSince 가 cutoffAt 이전이거나 NULL(마이그레이션 이전 건) 인 경우를 모두 포함.
     */
    List<LoanReview> findByRevStatusCdAndPendingApproverSinceBeforeAndDeletedAtIsNull(
            String revStatusCd, OffsetDateTime cutoffAt);

    List<LoanReview> findByRevStatusCdAndPendingApproverSinceIsNullAndDeletedAtIsNull(
            String revStatusCd);

    List<LoanReview> findByReviewedAtGreaterThanEqualAndReviewedAtLessThanAndDeletedAtIsNull(
            OffsetDateTime fromInclusive, OffsetDateTime toExclusive);

    List<LoanReview> findByReviewerIdAndReviewedAtGreaterThanEqualAndDeletedAtIsNull(
            Long reviewerId, OffsetDateTime since);

    @Query("""
            SELECT lr FROM LoanReview lr
            WHERE lr.updatedAt >= :since
              AND lr.revDecisionCd IS NOT NULL
              AND lr.deletedAt IS NULL
            ORDER BY lr.updatedAt ASC
            """)
    List<LoanReview> findExportable(@Param("since") OffsetDateTime since);
}
