package com.bank.docagent.kafka;

import com.bank.docagent.kafka.event.FraudAuditEvent;
import com.bank.docagent.kafka.event.RoutedEvent;
import com.bank.docagent.submission.event.ExtractionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${doc-agent.kafka.topics.extracted}")   private String extractedTopic;
    @Value("${doc-agent.kafka.topics.routed}")       private String routedTopic;
    @Value("${doc-agent.kafka.topics.fraud-audit}")  private String fraudAuditTopic;

    public void publishExtracted(ExtractionCompletedEvent event) {
        send(extractedTopic, event.submissionId().toString(), event, "extracted");
    }

    public void publishRouted(RoutedEvent event) {
        send(routedTopic, event.submissionId().toString(), event, "routed");
    }

    public void publishFraudAudit(FraudAuditEvent event) {
        send(fraudAuditTopic, event.submissionId().toString(), event, "fraud-audit");
    }

    private void send(String topic, String key, Object payload, String label) {
        kafkaTemplate.send(topic, key, payload)
            .whenComplete((r, ex) -> {
                if (ex != null) {
                    log.error("{} 이벤트 발행 실패: key={}", label, key, ex);
                } else {
                    log.debug("{} 이벤트 발행: key={}", label, key);
                }
            });
    }
}
