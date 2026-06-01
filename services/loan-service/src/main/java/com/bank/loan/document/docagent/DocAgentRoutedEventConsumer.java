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
public class DocAgentRoutedEventConsumer {

    private final ObjectMapper objectMapper;
    private final LoanDocumentService loanDocumentService;

    @KafkaListener(
            topics = "doc-agent.routed",
            groupId = "loan-service-doc-agent",
            containerFactory = "docAgentListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            RoutedEvent event = objectMapper.readValue(record.value(), RoutedEvent.class);
            if (!RoutedEvent.EVENT_TYPE.equals(event.eventType())) {
                ack.acknowledge();
                return;
            }
            log.info("DocAgentRoutedEvent: submissionId={} verifyStatus={}",
                    event.submissionId(), event.verifyStatus());
            loanDocumentService.handleRoutedEvent(event.submissionId(), event.verifyStatus());
        } catch (JsonProcessingException e) {
            log.error("DocAgentRoutedEvent: JSON 역직렬화 실패 key={}", record.key(), e);
        } catch (Exception e) {
            log.error("DocAgentRoutedEvent: 처리 실패 key={}", record.key(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
