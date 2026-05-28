package com.bank.docagent.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CONFIRMED_FORGERY 확정 시 감사팀 이관용 Kafka 이벤트.
 * doc-agent.fraud.audit 토픽으로 발행.
 */
public record FraudAuditEvent(
    @JsonProperty("event_type")     String eventType,
    @JsonProperty("submission_id")  UUID submissionId,
    @JsonProperty("application_id") String applicationId,
    @JsonProperty("doc_code")       String docCode,
    @JsonProperty("reviewer_id")    String reviewerId,
    @JsonProperty("retention_until") LocalDate retentionUntil,
    @JsonProperty("occurred_at")    LocalDateTime occurredAt
) {
    public static FraudAuditEvent of(UUID submissionId, String applicationId,
                                     String docCode, String reviewerId,
                                     LocalDate retentionUntil) {
        return new FraudAuditEvent(
            "FRAUD_CONFIRMED",
            submissionId, applicationId, docCode, reviewerId,
            retentionUntil, LocalDateTime.now()
        );
    }
}
