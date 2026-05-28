package com.bank.docagent.submission.event;

import com.bank.docagent.submission.domain.DocumentSubmission.VerifyStatus;
import com.bank.docagent.submission.dto.OcrRegion;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExtractionCompletedEvent(
    @JsonProperty("event_type")     String eventType,
    @JsonProperty("submission_id")  UUID submissionId,
    @JsonProperty("application_id") String applicationId,
    @JsonProperty("doc_code")       String docCode,
    @JsonProperty("doc_type")       String docType,
    @JsonProperty("verify_status")  VerifyStatus verifyStatus,
    @JsonProperty("region_count")   int regionCount,
    @JsonProperty("masked_key")     String maskedKey,
    @JsonProperty("occurred_at")    LocalDateTime occurredAt
) {
    public static ExtractionCompletedEvent of(UUID submissionId, String applicationId,
                                              String docCode, String docType,
                                              VerifyStatus status, List<OcrRegion> regions,
                                              String maskedKey) {
        return new ExtractionCompletedEvent(
            "EXTRACTION_COMPLETED", submissionId, applicationId,
            docCode, docType, status, regions.size(), maskedKey, LocalDateTime.now()
        );
    }
}
