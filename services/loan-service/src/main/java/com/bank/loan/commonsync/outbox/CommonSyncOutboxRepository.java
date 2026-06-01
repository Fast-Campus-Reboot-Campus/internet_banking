package com.bank.loan.commonsync.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CommonSyncOutboxRepository extends JpaRepository<CommonSyncOutbox, Long> {

    /** dispatch 핫패스 — PENDING/FAILED 중 nextAttemptAt 도달한 후보 페이지 픽업. */
    List<CommonSyncOutbox> findByStatusInAndNextAttemptAtLessThanEqualAndDeletedAtIsNullOrderByNextAttemptAtAsc(
            List<String> statuses, OffsetDateTime now, Pageable pageable);

    Optional<CommonSyncOutbox> findByOutboxIdAndDeletedAtIsNull(Long outboxId);

    /** 멱등 키 중복 체크 — 적재 전 appender 가 호출. */
    Optional<CommonSyncOutbox> findByIdempotencyKeyAndDeletedAtIsNull(String idempotencyKey);
}
