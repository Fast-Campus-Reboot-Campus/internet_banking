package com.bank.loan.document.docagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FraudAuditEvent(
        @JsonProperty("event_type")      String eventType,
        @JsonProperty("submission_id")   String submissionId,
        @JsonProperty("application_id")  String applicationId,
        @JsonProperty("doc_code")        String docCode,
        @JsonProperty("reviewer_id")     String reviewerId,
        @JsonProperty("retention_until") String retentionUntil,
        @JsonProperty("occurred_at")     String occurredAt
) {
    public static final String EVENT_TYPE = "FRAUD_CONFIRMED";
}
