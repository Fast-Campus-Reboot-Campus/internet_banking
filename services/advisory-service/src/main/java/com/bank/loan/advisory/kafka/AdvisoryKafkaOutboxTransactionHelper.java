package com.bank.loan.advisory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka send() 완료 후 Outbox 상태를 별도 트랜잭션으로 갱신.
 *
 * OutboxPublisher에 @Transactional 금지 (Kafka send가 DB 트랜잭션 안에 들어가면
 * send 지연이 곧 DB 락 보유 시간이 됨). 상태 갱신만 이 Bean에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdvisoryKafkaOutboxTransactionHelper {

    private final AdvisoryKafkaOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(AdvisoryKafkaOutboxMessage message) {
        message.markPublished();
        repository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(AdvisoryKafkaOutboxMessage message) {
        message.markFailed();
        repository.save(message);
        log.error("[advisory-outbox] FAILED — id={} topic={} eventType={}",
                message.getId(), message.getTopic(), message.getEventType());
    }
}
