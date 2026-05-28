package com.bank.docagent.kafka;

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

    @Value("${doc-agent.kafka.topics.extracted}") private String extractedTopic;
    @Value("${doc-agent.kafka.topics.routed}")    private String routedTopic;

    public void publishExtracted(ExtractionCompletedEvent event) {
        kafkaTemplate.send(extractedTopic, event.submissionId().toString(), event)
            .whenComplete((r, ex) -> {
                if (ex != null) {
                    log.error("extracted 이벤트 발행 실패: submissionId={}", event.submissionId(), ex);
                } else {
                    log.debug("extracted 이벤트 발행: submissionId={}", event.submissionId());
                }
            });
    }
}
