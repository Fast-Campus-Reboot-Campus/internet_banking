package com.bank.loan.notification.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    /** dispatch 핫패스. PENDING/FAILED 중 nextAttemptAt <= now 인 row 를 페이지 단위로. */
    List<NotificationOutbox>
    findByStatusInAndNextAttemptAtLessThanEqualAndDeletedAtIsNullOrderByNextAttemptAtAsc(
            List<String> statuses, OffsetDateTime cutoff, Pageable pageable);

    Optional<NotificationOutbox> findByOutboxIdAndDeletedAtIsNull(Long outboxId);

    Optional<NotificationOutbox> findByIdempotencyKeyAndDeletedAtIsNull(String idempotencyKey);

    /** 운영자 조회. eventType / status 필터링은 호출자가 조합. findAll 무페이지 금지. */
    Page<NotificationOutbox> findByEventTypeCdAndStatusAndDeletedAtIsNull(
            String eventTypeCd, String status, Pageable pageable);

    Page<NotificationOutbox> findByEventTypeCdAndDeletedAtIsNull(String eventTypeCd, Pageable pageable);

    Page<NotificationOutbox> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<NotificationOutbox> findByDeletedAtIsNull(Pageable pageable);
}
