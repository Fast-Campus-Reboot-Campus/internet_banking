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
 * acks=all + enable.idempotence=true (application.yml) 로 브로커 레벨 중복 발행을 추가 차단.
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
