package com.bank.docagent.submission.dto;

import com.bank.docagent.submission.domain.DocumentSubmission.VerifyStatus;
import com.bank.docagent.submission.dto.extracted.StructuredData;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractionResult(
    @JsonProperty("schema_version")  String schemaVersion,
    @JsonProperty("submission_id")   UUID submissionId,
    @JsonProperty("application_id")  String applicationId,
    @JsonProperty("doc_code")        String docCode,
    @JsonProperty("doc_type")        String docType,
    @JsonProperty("verify_status")   VerifyStatus verifyStatus,
    @JsonProperty("ocr_regions")     List<OcrRegion> ocrRegions,
    @JsonProperty("masked_text")     String maskedText,
    @JsonProperty("extracted_data")  StructuredData extractedData,
    @JsonProperty("pipeline_stage")  String pipelineStage
) {
    public static ExtractionResult of(UUID submissionId, String applicationId,
                                      String docCode, String docType,
                                      List<OcrRegion> regions, String maskedText,
                                      StructuredData extractedData, VerifyStatus status) {
        return new ExtractionResult(
            "1.0", submissionId, applicationId, docCode, docType,
            status, regions, maskedText, extractedData, "L4_EXTRACT_COMPLETE"
        );
    }
}
