package com.bank.loan.advisory.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * L3 Avro + Schema Registry 설정.
 *
 * advisory.kafka.use-avro=true 일 때만 활성화.
 * String 기반 기존 빈과 별도로 존재 — 마이그레이션 전/후 비교 가능.
 *
 * 학습 순서:
 *   1. use-avro=false (String) 상태에서 메시지 발행/수신 확인
 *   2. use-avro=true 로 재기동 → Avro로 발행 시도
 *   3. Schema Registry REST로 등록된 스키마 확인
 *   4. v2 스키마(severity 필드 추가) 등록 → BACKWARD 호환성 검증
 *   5. 호환성 위반 스키마(필수 필드 삭제) 등록 시도 → 실패 확인
 */
@Configuration
@ConditionalOnProperty(name = "advisory.kafka.use-avro", havingValue = "true")
public class AdvisoryAvroConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${advisory.kafka.schema-registry-url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Bean("advisoryAvroKafkaTemplate")
    public KafkaTemplate<String, Object> advisoryAvroKafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(avroProducerProps()));
    }

    @Bean("advisoryAvroListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> advisoryAvroListenerContainerFactory(
            AdvisoryConsumerProperties props) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(avroConsumerProps()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(advisoryAvroKafkaTemplate());
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(recoverer,
                        new FixedBackOff(props.getDlqBackoffIntervalMs(), props.getDlqMaxAttempts())));

        return factory;
    }

    private Map<String, Object> avroProducerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "advisory-avro-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true); // 학습용. 운영에서는 false 권장
        // 신뢰성 설정은 String producer와 동일
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return props;
    }

    private Map<String, Object> avroConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "advisory-avro-consumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "advisory-quarantine-notifier-avro");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false); // GenericRecord 사용
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
