package com.bank.loan.advisory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * advisory.quarantine.triggered.v1 컨슈머.
 *
 * at-least-once 환경이므로 동일 메시지가 재전달될 수 있다.
 * (topic, partition, offset) UNIQUE 제약으로 중복 처리 방지.
 *
 * 처리 순서:
 *   1. 중복 체크 (이미 처리된 offset이면 ack 후 종료)
 *   2. 비즈니스 처리 (현재는 로그 — 실무라면 운영자 알림 발송)
 *   3. 처리 이력 저장 (UNIQUE 위반 시 중복으로 간주 → ack 후 종료)
 *   4. ack — DB commit 완료 후 호출
 *
 * 실패 시: DefaultErrorHandler가 3회 재시도 → DLQ(*.dlq)로 격리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdvisoryQuarantineKafkaConsumer {

    private final AdvisoryConsumedEventRepository consumedEventRepository;
    private final AdvisoryConsumerProperties consumerProperties;

    @KafkaListener(
            topics = AdvisoryKafkaOutboxMessage.TOPIC_QUARANTINE,
            groupId = "advisory-quarantine-notifier",
            containerFactory = "advisoryListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String topic     = record.topic();
        int    partition = record.partition();
        long   offset    = record.offset();
        String payload   = record.value();

        log.info("[quarantine-consumer] 수신 — topic={} partition={} offset={} key={}",
                topic, partition, offset, record.key());

        // 중복 체크
        if (consumedEventRepository.existsByTopicAndPartitionAndKafkaOffset(topic, partition, offset)) {
            log.warn("[quarantine-consumer] 중복 메시지 skip — partition={} offset={}", partition, offset);
            ack.acknowledge();
            return;
        }

        // 비즈니스 처리 (학습용: 로그만. 실무라면 운영자 알림 발송)
        processQuarantineEvent(payload);

        // 처리 이력 저장 — race condition은 UNIQUE 위반으로 잡아 무시
        try {
            consumedEventRepository.save(AdvisoryConsumedEvent.builder()
                    .topic(topic)
                    .partition(partition)
                    .kafkaOffset(offset)
                    .eventType("QUARANTINE_ALERT")
                    .aggregateId(record.key() != null ? record.key() : "unknown")
                    .consumedAt(OffsetDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.warn("[quarantine-consumer] 처리 이력 저장 중 중복 감지 — partition={} offset={}", partition, offset);
            ack.acknowledge();
            return;
        }

        // DB commit 완료 후 ack — 이 순서가 at-least-once 보장의 핵심
        ack.acknowledge();
        log.info("[quarantine-consumer] 처리 완료 — partition={} offset={}", partition, offset);
    }

    private void processQuarantineEvent(String payload) {
        log.warn("[quarantine-consumer] 격리 이벤트 처리 — payload={}", payload);

        // L7 실험: advisory.consumer.experiment-delay-ms 를 양수로 설정하면
        // 처리 지연을 시뮬레이션 → max.poll.interval.ms 초과 시 rebalance 발생 관찰
        long delayMs = consumerProperties.getExperimentDelayMs();
        if (delayMs > 0) {
            log.warn("[quarantine-consumer][실험] 처리 지연 {}ms 시뮬레이션 중...", delayMs);
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
