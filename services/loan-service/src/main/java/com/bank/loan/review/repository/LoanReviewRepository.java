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

    List<LoanReview> findByReviewedAtGreaterThanEqualAndReviewedAtLessThanAndDeletedAtIsNull(
            OffsetDateTime fromInclusive, OffsetDateTime toExclusive);
}
