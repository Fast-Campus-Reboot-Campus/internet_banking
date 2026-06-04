package com.bank.docagent.review.controller;

import com.bank.docagent.review.service.HumanReviewService;
import com.bank.docagent.submission.domain.DocumentSubmission;
import com.bank.docagent.submission.domain.DocumentSubmission.HumanReviewStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "심사원 검토", description = "HOLD 건 위변조 확정/해제 — 사람만 호출 가능")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class HumanReviewController {

    private final HumanReviewService reviewService;

    public record ReviewRequest(
        @JsonProperty("decision")   HumanReviewStatus decision,    // CLEARED | CONFIRMED_FORGERY
        @JsonProperty("reviewer_id") String reviewerId
    ) {}

    @Operation(summary = "심사원 위변조 확정/해제 (HOLD 건 전용)",
               description = "자동 해제 불가. CLEARED=통과, CONFIRMED_FORGERY=위변조 확정 후 감사팀 이관")
    @PostMapping("/{submissionId}/review")
    public ResponseEntity<Map<String, Object>> decide(
        @PathVariable UUID submissionId,
        @RequestBody  ReviewRequest req
    ) {
        DocumentSubmission result = reviewService.decide(submissionId, req.decision(), req.reviewerId());
        return ResponseEntity.ok(Map.of(
            "submission_id",       result.getSubmissionId(),
            "verify_status",       result.getVerifyStatus(),
            "human_review_status", result.getHumanReviewStatus(),
            "reviewer_id",         result.getReviewerId()
        ));
    }
}
