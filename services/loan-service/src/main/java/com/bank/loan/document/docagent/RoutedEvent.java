package com.bank.loan.document.docagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoutedEvent(
        @JsonProperty("event_type")     String eventType,
        @JsonProperty("submission_id")  String submissionId,
        @JsonProperty("application_id") String applicationId,
        @JsonProperty("doc_code")       String docCode,
        @JsonProperty("verify_status")  String verifyStatus,
        @JsonProperty("occurred_at")    OffsetDateTime occurredAt
) {
    public static final String EVENT_TYPE = "SUBMISSION_ROUTED";
}
