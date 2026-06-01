package com.bank.loan.rag.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * loan_review_outbox 리포지토리 — Phase E (E3-4).
 */
public interface LoanReviewOutboxRepository extends JpaRepository<LoanReviewOutbox, Long> {

    Optional<LoanReviewOutbox> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    /** polling worker — 발행 대기(PENDING/FAILED) 중 재시도 시각 도달 건. */
    List<LoanReviewOutbox> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            @Param("statuses") List<String> statuses, @Param("now") OffsetDateTime now);

    /** index lag 모니터 — 주어진 status 중 가장 오래된(created_at ↑) 1건. */
    Optional<LoanReviewOutbox> findTopByStatusOrderByCreatedAtAsc(String status);
}
