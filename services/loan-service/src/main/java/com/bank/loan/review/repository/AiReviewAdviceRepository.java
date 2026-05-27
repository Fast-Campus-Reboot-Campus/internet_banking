package com.bank.loan.review.repository;

import com.bank.loan.review.domain.AiReviewAdvice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiReviewAdviceRepository extends JpaRepository<AiReviewAdvice, Long> {

    List<AiReviewAdvice> findByRevIdAndAdviceTypeCdOrderByCreatedAtDesc(Long revId, String adviceTypeCd);

    List<AiReviewAdvice> findByRevIdOrderByCreatedAtDesc(Long revId);

    Optional<AiReviewAdvice> findFirstByRevIdAndAdviceTypeCdOrderByCreatedAtDesc(Long revId, String adviceTypeCd);
}
