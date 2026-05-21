package com.bank.loan.review.repository;

import com.bank.loan.review.domain.LoanReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanReviewRepository extends JpaRepository<LoanReview, Long> {

    Optional<LoanReview> findByApplIdAndDeletedAtIsNull(Long applId);
}
