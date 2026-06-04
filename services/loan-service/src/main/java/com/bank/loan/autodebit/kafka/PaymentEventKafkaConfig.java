package com.bank.loan.autodebit.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
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
 * payment-service 결제 결과 이벤트(payment.completed / payment.failed) 구독용 Kafka 설정.
 * 타행 청산(CLEARING) 완결·실패 통보를 수신해 자동이체 상환을 완결한다.
 */
@Configuration
public class PaymentEventKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> paymentEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                 "loan-service-payment-event");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> paymentEventListenerContainerFactory(
            KafkaTemplate<String, String> kafkaTemplate) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(paymentEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(1);
        factory.setCommonErrorHandler(paymentEventErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * 청산 결과 처리 실패 시 재시도(2초 간격 3회) 후 소진되면 {@code <topic>.DLT} 로 라우팅한다.
     * 무한 재시도로 파티션을 막지 않으면서도, 실패한 청산 결과가 조용히 유실되지 않게 한다.
     */
    private DefaultErrorHandler paymentEventErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3L));
    }
}
