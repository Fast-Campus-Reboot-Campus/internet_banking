package com.bank.loan.review.repository;

import com.bank.loan.review.domain.LoanReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanReviewRepository extends JpaRepository<LoanReview, Long> {

    Optional<LoanReview> findByApplIdAndDeletedAtIsNull(Long applId);

    List<LoanReview> findByRevStatusCdAndDeletedAtIsNullOrderByReviewedAtAsc(String revStatusCd);

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
}
