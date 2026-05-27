package com.bank.loan.advisory.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Transactional Outbox 워커.
 *
 * advisory_kafka_outbox.status=PENDING 레코드를 5초마다 폴링해 Kafka로 발행한다.
 * @Transactional 금지: Kafka send()가 DB 트랜잭션 안에 들어가면 send 지연이 락 보유 시간이 됨.
 * DB 상태 갱신은 AdvisoryKafkaOutboxTransactionHelper(별도 Bean)에 위임한다.
 *
 * L3 실험: advisory.kafka.use-avro=true 시 Avro 직렬화로 발행.
 * Outbox는 계속 JSON으로 저장 (가독성·DB 쿼리 가능) — 발행 시점에만 Avro 변환.
 */
@Slf4j
@Component
public class AdvisoryKafkaOutboxPublisher {

    @Value("${advisory.kafka.use-avro:false}")
    private boolean useAvro;

    private final AdvisoryKafkaOutboxRepository repository;
    private final AdvisoryKafkaOutboxTransactionHelper transactionHelper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Optional<KafkaTemplate<String, Object>> avroKafkaTemplate;
    private final Optional<AdvisoryAvroConverter> avroConverter;

    public AdvisoryKafkaOutboxPublisher(
            AdvisoryKafkaOutboxRepository repository,
            AdvisoryKafkaOutboxTransactionHelper transactionHelper,
            @Qualifier("advisoryKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Qualifier("advisoryAvroKafkaTemplate")
            Optional<KafkaTemplate<String, Object>> avroKafkaTemplate,
            Optional<AdvisoryAvroConverter> avroConverter) {
        this.repository = repository;
        this.transactionHelper = transactionHelper;
        this.kafkaTemplate = kafkaTemplate;
        this.avroKafkaTemplate = avroKafkaTemplate;
        this.avroConverter = avroConverter;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPending() {
        List<AdvisoryKafkaOutboxMessage> pending = repository.findPending();
        if (pending.isEmpty()) {
            return;
        }
        log.debug("[advisory-outbox] 발행 대상: {}건 (avro={})", pending.size(), useAvro);
        for (AdvisoryKafkaOutboxMessage message : pending) {
            publishOne(message);
        }
    }

    private void publishOne(AdvisoryKafkaOutboxMessage message) {
        try {
            if (useAvro && avroKafkaTemplate.isPresent() && avroConverter.isPresent()) {
                publishAvro(message);
            } else {
                publishString(message);
            }
            transactionHelper.markPublished(message);
            log.debug("[advisory-outbox] 발행 완료 — id={} topic={} avro={}",
                    message.getId(), message.getTopic(), useAvro);
        } catch (ExecutionException e) {
            log.error("[advisory-outbox] 발행 실패 — id={} topic={} error={}",
                    message.getId(), message.getTopic(), e.getCause().getMessage(), e);
            transactionHelper.markFailed(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[advisory-outbox] 인터럽트 — id={}", message.getId(), e);
            transactionHelper.markFailed(message);
        }
    }

    private void publishString(AdvisoryKafkaOutboxMessage message)
            throws ExecutionException, InterruptedException {
        kafkaTemplate.send(message.getTopic(), message.getRecordKey(), message.getPayloadJson()).get();
    }

    private void publishAvro(AdvisoryKafkaOutboxMessage message)
            throws ExecutionException, InterruptedException {
        GenericRecord record = avroConverter.get()
                .toGenericRecord(message.getTopic(), message.getPayloadJson());
        avroKafkaTemplate.get().send(message.getTopic(), message.getRecordKey(), record).get();
    }
}
