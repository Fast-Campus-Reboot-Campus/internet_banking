package com.bank.loan.advisory.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * L3 Avro consumer — advisory.quarantine.triggered.v1 구독.
 *
 * advisory.kafka.use-avro=true 일 때만 활성화.
 * String consumer(advisory-quarantine-notifier)와 다른 group-id 사용
 * → 동일 토픽을 두 consumer가 독립적으로 읽으므로 String/Avro 동시 비교 가능.
 *
 * 학습 포인트:
 *   - GenericRecord로 필드 접근 (record.get("fieldName"))
 *   - v2 스키마 등록 후 severity 필드 접근 시도
 *   - 호환성 위반 스키마 등록 시도 → Schema Registry 거부 확인
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "advisory.kafka.use-avro", havingValue = "true")
public class AdvisoryAvroQuarantineConsumer {

    @KafkaListener(
            topics = AdvisoryKafkaOutboxMessage.TOPIC_QUARANTINE,
            groupId = "advisory-quarantine-notifier-avro",
            containerFactory = "advisoryAvroListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        GenericRecord avroRecord = (GenericRecord) record.value();

        log.info("[avro-consumer] 수신 — partition={} offset={}", record.partition(), record.offset());
        log.info("[avro-consumer] revId={} conclusionCd={} analysisType={}",
                avroRecord.get("revId"),
                avroRecord.get("conclusionCd"),
                avroRecord.get("analysisType"));

        // v2 스키마 실험: severity 필드가 있으면 출력
        // Schema Registry에 v2 등록 후 producer가 v2로 발행하면 이 필드가 채워짐
        if (avroRecord.getSchema().getField("severity") != null) {
            log.info("[avro-consumer] severity={}", avroRecord.get("severity"));
        }

        ack.acknowledge();
    }
}
