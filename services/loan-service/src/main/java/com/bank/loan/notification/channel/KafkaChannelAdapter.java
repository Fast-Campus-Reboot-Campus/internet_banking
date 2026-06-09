package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka 도메인 이벤트 발행 채널 어댑터.
 *
 * outbox row 의 idempotencyKey 를 Kafka 메시지 키로 사용한다.
 *   key = "LOAN_DISBURSED:{cntrId}:KAFKA_DOMAIN_EVENT"
 *
 * 같은 key 로 발행된 메시지는 동일 파티션에 순서 보장되며,
 * 컨슈머 측에서 key 기반 중복 처리가 가능하다.
 *
 * 이 producer 는 Spring Boot 가 자동 설정한 KafkaTemplate 을 그대로 쓴다.
 * application.yml 에 spring.kafka.producer 설정이 없어, kafka-clients 3.x 기본값
 * (enable.idempotence=true → acks=all 강제)에 의존해 브로커 레벨 중복 발행이 차단된다.
 * 명시 설정이 아니라 라이브러리 디폴트에 의존하는 상태이므로, producer 설정을
 * yml 에 추가할 때 acks/idempotence 를 낮추지 않도록 주의.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChannelAdapter implements NotificationChannelAdapter {

    public static final String CHANNEL_CD = "KAFKA_DOMAIN_EVENT";
    public static final String TOPIC      = "loan-domain-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String getChannelCd() {
        return CHANNEL_CD;
    }

    @Override
    public SendResult send(NotificationOutbox outbox) {
        // idempotencyKey 를 Kafka 메시지 키로 사용 — 파티션 내 순서 보장 + 컨슈머 측 중복 판별
        String messageKey = outbox.getIdempotencyKey();
        try {
            kafkaTemplate.send(TOPIC, messageKey, outbox.getPayload())
                    .get(5, TimeUnit.SECONDS);
            log.debug("[kafka-channel] published topic={} key={}", TOPIC, messageKey);
            return new SendResult(true, messageKey, "OK", null);
        } catch (ExecutionException e) {
            log.warn("[kafka-channel] publish failed key={}: {}", messageKey, e.getCause().getMessage());
            return new SendResult(false, null, "ERR", e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SendResult(false, null, "INTERRUPTED", e.getMessage());
        } catch (TimeoutException e) {
            return new SendResult(false, null, "TIMEOUT", "5s timeout exceeded");
        }
    }
}
