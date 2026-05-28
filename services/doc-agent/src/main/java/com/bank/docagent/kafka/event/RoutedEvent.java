package com.bank.docagent.kafka.event;

import com.bank.docagent.submission.domain.DocumentSubmission.VerifyStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 최종 라우팅 결과 이벤트 (AUTO_PASS / NEEDS_RESUBMIT / HOLD).
 * doc-agent.routed 토픽으로 발행.
 */
public record RoutedEvent(
    @JsonProperty("event_type")     String eventType,
    @JsonProperty("submission_id")  UUID submissionId,
    @JsonProperty("application_id") String applicationId,
    @JsonProperty("doc_code")       String docCode,
    @JsonProperty("verify_status")  VerifyStatus verifyStatus,
    @JsonProperty("occurred_at")    LocalDateTime occurredAt
) {
    public static RoutedEvent of(UUID submissionId, String applicationId,
                                 String docCode, VerifyStatus status) {
        return new RoutedEvent(
            "SUBMISSION_ROUTED",
            submissionId, applicationId, docCode, status, LocalDateTime.now()
        );
    }
}
