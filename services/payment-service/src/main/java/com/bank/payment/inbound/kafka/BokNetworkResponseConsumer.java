package com.bank.payment.inbound.kafka;

import com.bank.payment.domain.BokSettlementTransaction;
import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.domain.service.PaymentTransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * BOK 망 응답 Consumer. @EnableKafka는 KftcKafkaConfig에 선언돼 있으므로 재선언 금지.
 * orchestrator 미주입 — 완결(SETTLEMENT_COMPLETED)은 txService 직접 호출.
 * F3 거절(SETTLEMENT_REJECT)은 단계3 구현 예정.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BokNetworkResponseConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentTransactionService txService;

    @KafkaListener(
            topics = "bok.network.response",
            containerFactory = "bokListenerContainerFactory",
            groupId = "${payment.kafka.bok.consumer-group}"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        String messageType = payload.path("messageType").asText();

        switch (messageType) {
            case "SETTLEMENT_COMPLETED": {
                String bokReferenceNo = payload.path("bokReferenceNo").asText();
                String responseCode   = payload.path("responseCode").asText();
                if (!"0000".equals(responseCode)) {
                    log.warn("[BOK] SETTLEMENT_COMPLETED 비정상 responseCode: {} bokReferenceNo={}",
                            responseCode, bokReferenceNo);
                }

                BokSettlementTransaction bst = txService.selectByBokReferenceNo(bokReferenceNo);
                if (bst == null) {
                    log.error("[BOK] SETTLEMENT_COMPLETED BST 없음, skip. bokReferenceNo={}", bokReferenceNo);
                    break;
                }
                String piId = bst.getOurPaymentInstructionId();
                PaymentInstruction pi = txService.selectById(piId);
                if (pi == null) {
                    log.error("[BOK] SETTLEMENT_COMPLETED PI 없음, skip. bokReferenceNo={} piId={}",
                            bokReferenceNo, piId);
                    break;
                }

                // 멱등 가드 1: 이미 COMPLETED (중복수신)
                if ("COMPLETED".equals(pi.getStatus())) {
                    log.info("[BOK] SETTLEMENT_COMPLETED 중복수신 skip(이미 COMPLETED). piId={}", piId);
                    break;
                }
                // 멱등 가드 2: CLEARING 아닌 상태 (예상치 못한 전이)
                if (!"CLEARING".equals(pi.getStatus())) {
                    log.warn("[BOK] SETTLEMENT_COMPLETED CLEARING 아닌 상태, skip. piId={} status={}",
                            piId, pi.getStatus());
                    break;
                }

                String settledAt      = payload.path("settledAt").asText();
                String settlementDate = settledAt.length() >= 8 ? settledAt.substring(0, 8) : null;
                txService.txSettlementBok(pi, bokReferenceNo, settledAt, settlementDate);
                log.info("[BOK] SETTLEMENT_COMPLETED 처리완료. piId={} CLEARING→COMPLETED", piId);
                break;
            }
            case "SETTLEMENT_REJECT":
                // 단계3(F3) TODO
                log.warn("[BOK] SETTLEMENT_REJECT 미구현(단계3). bokReferenceNo={}",
                        payload.path("bokReferenceNo").asText());
                break;
            default:
                log.warn("[BOK] unknown messageType: messageType={} key={}", messageType, record.key());
        }

        log.info("BOK response received: key={} messageType={} partition={} offset={}",
                record.key(), messageType, record.partition(), record.offset());

        // ★ DB COMMIT 후 ack. 예외 시 미호출 → DLQ 라우팅(BokKafkaConfig 3회 재시도)
        ack.acknowledge();
    }
}
