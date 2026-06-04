package com.bank.loan.autodebit.kafka;

import com.bank.loan.autodebit.service.ClearingResultService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * payment-service 결제 결과 이벤트 소비자.
 *
 * 구독 토픽:
 *   - payment.completed : PAYMENT_COMPLETED(status="COMPLETED") / KFTC_SETTLED / BOK_CONFIRMED
 *   - payment.failed    : PAYMENT_FAILED(status="FAILED")
 *
 * payment 이벤트에는 idempotencyKey 가 없고 paymentInstructionId(piId) 만 실린다.
 * piId 로 자동이체 청산 대기건을 찾아 상환을 완결한다.
 * payment.completed 중 status 가 없는 이벤트(KFTC_SETTLED/BOK_CONFIRMED)는 회계계용이라 무시한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final ClearingResultService clearingResultService;

    @KafkaListener(
            topics = {"payment.completed", "payment.failed"},
            groupId = "loan-service-payment-event",
            containerFactory = "paymentEventListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String piId = node.path("paymentInstructionId").asText("");
            String status = node.path("status").asText("");

            // piId 없음(역분개 등 다른 스키마) 또는 status 없음(KFTC_SETTLED/BOK_CONFIRMED) → 무시
            // (ack 는 finally 에서 일괄 처리)
            if (piId.isBlank() || status.isBlank()) {
                return;
            }

            boolean completed = "COMPLETED".equals(status);
            boolean failed = "FAILED".equals(status);
            if (!completed && !failed) {
                return;
            }

            clearingResultService.handle(piId, completed);
        } catch (Exception e) {
            log.error("payment event 처리 실패 topic={} key={}", record.topic(), record.key(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
