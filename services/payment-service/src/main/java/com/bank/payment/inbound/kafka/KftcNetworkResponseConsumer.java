package com.bank.payment.inbound.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KftcNetworkResponseConsumer {

    private final ObjectMapper objectMapper;

    @Value("${payment.bank-code:004}")
    private String bankCode;

    @KafkaListener(
            topics = "kftc.network.response",
            containerFactory = "kftcListenerContainerFactory",
            groupId = "${payment.kafka.kftc.consumer-group}"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        String responseType = payload.path("responseType").asText();

        // TODO P-029 self-listening 방지: senderBankCode가 자행 bankCode와 같으면 ack+skip
        // String senderBankCode = payload.path("senderBankCode").asText();
        // if (bankCode.equals(senderBankCode)) {
        //     log.debug("P-029 self-listening skip: senderBankCode={}", senderBankCode);
        //     ack.acknowledge();
        //     return;
        // }

        switch (responseType) {
            case "REJECT":
            case "PAYMENT_REJECT":
                // TODO F2 보상: PaymentOrchestrator.processKftcReject() 미구현 (S2-A/F2에서 구현)
                break;
            case "SETTLEMENT_NOTIFY":
                // TODO 청산완료 처리 미구현 (S2-A/F2에서 구현)
                break;
            default:
                log.warn("KFTC unknown responseType: responseType={} key={}", responseType, record.key());
        }

        log.info("KFTC response received: key={} responseType={} partition={} offset={}",
                record.key(), responseType, record.partition(), record.offset());

        // ★ DB COMMIT 후 ack (현재 골격은 처리 없으니 파싱 후 바로). 실패 시 미호출 → DLQ
        ack.acknowledge();
    }
}
