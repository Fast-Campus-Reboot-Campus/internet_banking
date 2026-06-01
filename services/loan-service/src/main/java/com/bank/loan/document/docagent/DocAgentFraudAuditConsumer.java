package com.bank.loan.document.docagent;

import com.bank.loan.document.service.LoanDocumentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocAgentFraudAuditConsumer {

    private final ObjectMapper objectMapper;
    private final LoanDocumentService loanDocumentService;

    @KafkaListener(
            topics = "doc-agent.fraud.audit",
            groupId = "loan-service-doc-agent",
            containerFactory = "docAgentListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            FraudAuditEvent event = objectMapper.readValue(record.value(), FraudAuditEvent.class);
            if (!FraudAuditEvent.EVENT_TYPE.equals(event.eventType())) {
                ack.acknowledge();
                return;
            }
            log.info("DocAgentFraudAudit: submissionId={} applicationId={}",
                    event.submissionId(), event.applicationId());
            loanDocumentService.handleFraudAuditEvent(
                    event.submissionId(), event.applicationId(), event.retentionUntil());
        } catch (JsonProcessingException e) {
            log.error("DocAgentFraudAudit: JSON 역직렬화 실패 key={}", record.key(), e);
        } catch (Exception e) {
            log.error("DocAgentFraudAudit: 처리 실패 key={}", record.key(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
