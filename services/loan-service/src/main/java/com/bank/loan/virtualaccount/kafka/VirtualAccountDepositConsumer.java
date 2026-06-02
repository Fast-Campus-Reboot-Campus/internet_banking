package com.bank.loan.virtualaccount.kafka;

import com.bank.loan.virtualaccount.service.VirtualAccountDepositService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 가상계좌 입금통지 소비자.
 *
 * payment.completed 에서 인바운드 입금(direction=IN, status=COMPLETED)만 골라 가상계좌 상환 처리로 보낸다.
 * 자동이체 CLEARING 소비자({@code PaymentEventConsumer})와 다른 groupId 를 써, 같은 토픽을 독립 구독한다.
 *
 * ⚠️ 처리 측은 STUB — {@link VirtualAccountDepositService} 참고(결제계 receiverAccountNo 변경 대기).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VirtualAccountDepositConsumer {

    private final ObjectMapper objectMapper;
    private final VirtualAccountDepositService virtualAccountDepositService;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "loan-service-virtual-account",
            containerFactory = "paymentEventListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            boolean inbound = "IN".equals(node.path("direction").asText(""));
            boolean completed = "COMPLETED".equals(node.path("status").asText(""));
            // 인바운드 입금 완결만 관심. OUT(자동이체 등)·status 없는 이벤트는 무시.
            if (inbound && completed) {
                virtualAccountDepositService.handleInboundDeposit(node);
            }
        } catch (Exception e) {
            log.error("가상계좌 입금통지 처리 실패 key={}", record.key(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
