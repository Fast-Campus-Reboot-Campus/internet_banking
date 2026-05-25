package com.bank.loan.notification.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 알림 outbox 적재 헬퍼.
 *
 * 모든 listener 가 이 한 진입점으로 outbox 를 쌓는다. 직접 repository 를 다루지 않게 해
 * idempotency·status·백오프 초기값 같은 표준을 listener 마다 반복하지 않게 한다.
 *
 * 트랜잭션 두 가지:
 *   enqueueInCurrentTx — 호출자의 기존 트랜잭션에 참여 (순수 Outbox 패턴).
 *                        도메인 저장과 outbox INSERT 가 원자적으로 commit 된다.
 *   enqueue            — REQUIRES_NEW 로 새 트랜잭션 생성. AFTER_COMMIT 비동기 리스너에서 사용.
 *
 * 멱등:
 *   idempotencyKey = eventTypeCd + ":" + referenceId + ":" + channelCd (UNIQUE 제약).
 *   동일 키가 이미 존재하면 skip. 동시 삽입 race 는 DIVE 로 잡아 무시한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxAppender {

    private final NotificationOutboxRepository repository;

    /**
     * 순수 Outbox 패턴 — 호출자 트랜잭션에 참여.
     * 도메인 저장(loan_execution INSERT 등)과 같은 트랜잭션 안에서 outbox 를 적재해
     * 서버 크래시 시에도 데이터 정합성을 보장한다.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueueInCurrentTx(String eventTypeCd, Long referenceId, String channelCd, String payload) {
        String key = NotificationOutbox.idempotencyKeyOf(eventTypeCd, referenceId, channelCd);
        if (repository.findByIdempotencyKeyAndDeletedAtIsNull(key).isPresent()) {
            log.debug("[noti-outbox] skip duplicate key={}", key);
            return;
        }
        repository.save(buildOutbox(eventTypeCd, referenceId, channelCd, payload, key));
        log.debug("[noti-outbox] enqueued in-tx key={}", key);
    }

    /**
     * 비동기 리스너용 — REQUIRES_NEW 로 별도 트랜잭션.
     * AFTER_COMMIT 이후에 호출되므로 자체 트랜잭션이 필요하다.
     * 동시 삽입 race 는 DIVE 로 잡아 무시한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String eventTypeCd, Long referenceId, String channelCd, String payload) {
        String key = NotificationOutbox.idempotencyKeyOf(eventTypeCd, referenceId, channelCd);
        if (repository.findByIdempotencyKeyAndDeletedAtIsNull(key).isPresent()) {
            log.debug("[noti-outbox] skip duplicate key={}", key);
            return;
        }
        try {
            repository.save(buildOutbox(eventTypeCd, referenceId, channelCd, payload, key));
        } catch (DataIntegrityViolationException race) {
            log.debug("[noti-outbox] race on key={}", key);
        }
    }

    private NotificationOutbox buildOutbox(String eventTypeCd, Long referenceId,
                                           String channelCd, String payload, String key) {
        return NotificationOutbox.builder()
                .eventTypeCd(eventTypeCd)
                .referenceId(referenceId)
                .channelCd(channelCd)
                .payload(payload)
                .status(NotificationOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(NotificationOutbox.DEFAULT_MAX_ATTEMPT)
                .nextAttemptAt(OffsetDateTime.now())
                .idempotencyKey(key)
                .build();
    }
}
