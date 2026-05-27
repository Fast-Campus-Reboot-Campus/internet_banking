package com.bank.loan.advisory.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * advisory 전용 Kafka Producer 설정.
 *
 * Spring Boot auto-config KafkaTemplate(loan-notification 공유)과 분리해
 * advisory 도메인 요건에 맞는 producer를 독립적으로 구성한다.
 *
 * 주요 결정:
 *   - enable.idempotence=true + max.in.flight=5: 순서 보장 + 중복 방지
 *   - acks=all: ISR 전체 응답 대기 — 무손실
 *   - compression.type=lz4: JSON 페이로드 압축. snappy/zstd 대비 CPU 비용 최저
 *   - linger.ms=20: 20ms 모아서 배치 전송. Outbox 폴링(5s)과 조합 시 처리량↑
 *   - batch.size=32768: 기본(16KB)의 2배. 작은 메시지 다수 전송 시 효율화
 *   - buffer.memory=32MB: 브로커 응답 지연 시 프로듀서 측 버퍼링 한계
 *   - delivery.timeout.ms=120000: retries=MAX_VALUE이므로 이 값으로 재시도 상한 제어
 */
@Configuration
public class AdvisoryKafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // L4 실험: true로 바꾸면 SkewAwarePartitioner 활성화
    @Value("${advisory.producer.use-skew-aware-partitioner:false}")
    private boolean useSkewAwarePartitioner;

    @Bean("advisoryProducerFactory")
    public ProducerFactory<String, String> advisoryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean("advisoryKafkaTemplate")
    public KafkaTemplate<String, String> advisoryKafkaTemplate() {
        return new KafkaTemplate<>(advisoryProducerFactory());
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();

        // 브로커 연결
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "advisory-producer");

        // 직렬화
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // 신뢰성 — 무손실
        props.put(ProducerConfig.ACKS_CONFIG,                           "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,             true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);   // idempotence와 함께 5까지 순서 보장
        props.put(ProducerConfig.RETRIES_CONFIG,                        Integer.MAX_VALUE);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,            120_000); // retries 상한 제어

        // 압축 (lz4: 저비용 고효율. snappy/zstd/gzip 비교 실험 시 이 값 변경)
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // 배치 튜닝 (linger.ms=0 vs 20 처리량 비교 실험 시 이 값 변경)
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);   // 32KB
        props.put(ProducerConfig.LINGER_MS_CONFIG,  20);       // 20ms 모아서 전송

        // L4 실험: advisory.producer.use-skew-aware-partitioner=true 시 활성화
        if (useSkewAwarePartitioner) {
            props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, SkewAwarePartitioner.class.getName());
        }

        // 버퍼/소켓
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG,       33_554_432); // 32MB
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,     1_048_576); // 1MB
        props.put(ProducerConfig.SEND_BUFFER_CONFIG,           131_072);  // 128KB
        props.put(ProducerConfig.RECEIVE_BUFFER_CONFIG,         65_536);  // 64KB

        return props;
    }
}
