package com.bank.loan.advisory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Kafka Outbox 적재 헬퍼.
 *
 * enqueue() — REQUIRES_NEW 트랜잭션. AFTER_COMMIT 비동기 리스너 또는 동기 @EventListener에서 사용.
 * enqueueInCurrentTx() — 호출자 트랜잭션 참여. 도메인 저장과 원자적으로 적재할 때 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisoryKafkaOutboxAppender {

    private final AdvisoryKafkaOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String eventType, String aggregateId, String topic, String recordKey, String payload) {
        try {
            repository.save(build(eventType, aggregateId, topic, recordKey, payload));
            log.debug("[advisory-kafka-outbox] enqueued — eventType={} aggregateId={}", eventType, aggregateId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[advisory-kafka-outbox] race on eventType={} aggregateId={}", eventType, aggregateId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueueInCurrentTx(String eventType, String aggregateId, String topic, String recordKey, String payload) {
        repository.save(build(eventType, aggregateId, topic, recordKey, payload));
        log.debug("[advisory-kafka-outbox] enqueued in-tx — eventType={} aggregateId={}", eventType, aggregateId);
    }

    private AdvisoryKafkaOutboxMessage build(String eventType, String aggregateId,
                                             String topic, String recordKey, String payload) {
        return AdvisoryKafkaOutboxMessage.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .topic(topic)
                .recordKey(recordKey)
                .payloadJson(payload)
                .status(AdvisoryKafkaOutboxMessage.STATUS_PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
