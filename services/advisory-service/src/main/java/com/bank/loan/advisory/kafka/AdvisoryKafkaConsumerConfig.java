package com.bank.loan.advisory.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * advisory 전용 Kafka Consumer 설정.
 *
 * 튜닝 파라미터는 AdvisoryConsumerProperties(application.yml)로 외부화.
 * 재컴파일 없이 값 변경 → 재기동만으로 실험 가능.
 *
 * 주요 결정:
 *   - enable.auto.commit=false + MANUAL_IMMEDIATE: DB 처리 완료 후 ack → at-least-once 보장
 *   - auto.offset.reset=earliest: 최초 구독 시 과거 메시지 전부 읽기 (감사 이벤트 유실 방지)
 *   - isolation.level=read_committed: producer 트랜잭션 도입 시 커밋된 메시지만 읽기
 *   - DLQ: N회 재시도(간격 설정 가능) 후 *.dlq 토픽으로 격리
 */
@Configuration
public class AdvisoryKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean("advisoryConsumerFactory")
    public ConsumerFactory<String, String> advisoryConsumerFactory(AdvisoryConsumerProperties props) {
        return new DefaultKafkaConsumerFactory<>(consumerProps(props));
    }

    @Bean("advisoryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> advisoryListenerContainerFactory(
            @Qualifier("advisoryConsumerFactory") ConsumerFactory<String, String> consumerFactory,
            @Qualifier("advisoryKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            AdvisoryConsumerProperties props) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // DLQ: N회 재시도 → *.dlq 토픽으로 이동
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(props.getDlqBackoffIntervalMs(), props.getDlqMaxAttempts()));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    private Map<String, Object> consumerProps(AdvisoryConsumerProperties props) {
        Map<String, Object> p = new HashMap<>();

        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers);
        p.put(ConsumerConfig.CLIENT_ID_CONFIG,          "advisory-quarantine-consumer");
        p.put(ConsumerConfig.GROUP_ID_CONFIG,           "advisory-quarantine-notifier");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,  false);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,  "earliest");
        p.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,    "read_committed");

        // 튜닝 파라미터 — application.yml advisory.consumer.* 에서 주입
        // L8: Partition Assignment Strategy (재기동만으로 전략 교체 실험 가능)
        p.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, props.getAssignmentStrategy());

        // L8: Static Group Membership (null이면 동적 멤버십 유지)
        if (props.getGroupInstanceId() != null && !props.getGroupInstanceId().isBlank()) {
            p.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, props.getGroupInstanceId());
        }

        // L8: Rack Awareness (비어있으면 미적용)
        if (!props.getClientRack().isBlank()) {
            p.put(ConsumerConfig.CLIENT_RACK_CONFIG, props.getClientRack());
        }

        p.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,               props.getFetchMinBytes());
        p.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,             props.getFetchMaxWaitMs());
        p.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG,               props.getFetchMaxBytes());
        p.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,     props.getMaxPartitionFetchBytes());
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,              props.getMaxPollRecords());
        p.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,          props.getMaxPollIntervalMs());
        p.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,            props.getSessionTimeoutMs());
        p.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,         props.getHeartbeatIntervalMs());
        p.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG,            props.getRequestTimeoutMs());
        p.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,        props.getDefaultApiTimeoutMs());

        return p;
    }
}
