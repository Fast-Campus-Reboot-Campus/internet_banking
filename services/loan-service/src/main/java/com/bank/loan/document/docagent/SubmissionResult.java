package com.bank.loan.document.docagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionResult(
        @JsonProperty("submission_id")        String submissionId,
        @JsonProperty("application_id")       String applicationId,
        @JsonProperty("doc_code")             String docCode,
        @JsonProperty("verify_status")        String verifyStatus,
        @JsonProperty("document_verification") DocumentVerification documentVerification
) {
    public static final String VERIFY_AUTO_PASS      = "AUTO_PASS";
    public static final String VERIFY_NEEDS_RESUBMIT = "NEEDS_RESUBMIT";
    public static final String VERIFY_HOLD           = "HOLD";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentVerification(
            @JsonProperty("confidence_score") Double confidenceScore
    ) {}
}
