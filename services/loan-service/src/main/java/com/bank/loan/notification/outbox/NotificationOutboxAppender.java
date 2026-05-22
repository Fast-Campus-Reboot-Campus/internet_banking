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
 * 트랜잭션:
 *   listener 가 AFTER_COMMIT + @Async 이므로 본 메서드는 새 트랜잭션에서 INSERT 한 다음 즉시 commit.
 *   AI_GUIDELINES: 외부 API 호출은 이 안에서 일어나지 않는다. 송신은 dispatch 배치 책임.
 *
 * 멱등:
 *   동일 idempotencyKey 가 이미 있으면 skip — 동일 이벤트 재발행 시 row 중복 적재 방지.
 *   서비스 레벨 조회 race 가 일어나면 DB UNIQUE 제약이 잡고 DIVE 를 그대로 삼킨다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxAppender {

    private final NotificationOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String eventTypeCd, Long referenceId, String channelCd, String payload) {
        String key = NotificationOutbox.idempotencyKeyOf(eventTypeCd, referenceId, channelCd);
        if (repository.findByIdempotencyKeyAndDeletedAtIsNull(key).isPresent()) {
            log.debug("[noti-outbox] skip duplicate key={}", key);
            return;
        }
        try {
            repository.save(NotificationOutbox.builder()
                    .eventTypeCd(eventTypeCd)
                    .referenceId(referenceId)
                    .channelCd(channelCd)
                    .payload(payload)
                    .status(NotificationOutbox.STATUS_PENDING)
                    .attemptNo(0)
                    .maxAttempt(NotificationOutbox.DEFAULT_MAX_ATTEMPT)
                    .nextAttemptAt(OffsetDateTime.now())
                    .idempotencyKey(key)
                    .build());
        } catch (DataIntegrityViolationException race) {
            // 다른 thread 가 같은 키로 먼저 INSERT — 무시.
            log.debug("[noti-outbox] race on key={}", key);
        }
    }
}
