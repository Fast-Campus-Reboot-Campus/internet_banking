package com.bank.ai.review.controller;

import com.bank.ai.review.dto.AutoReviewRequest;
import com.bank.ai.review.dto.AutoReviewResponse;
import com.bank.ai.review.service.AutoReviewService;
import com.bank.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "auto-review", description = "자동 대출심사 추론")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AutoReviewController {

    private final AutoReviewService service;

    @Operation(summary = "자동심사 단건 추론",
            description = "신청자 피처를 받아 APPROVE/CONDITIONAL/REJECT 결정과 확률을 반환.")
    @PostMapping("/auto-review")
    public ApiResponse<AutoReviewResponse> review(@Valid @RequestBody AutoReviewRequest req) {
        return ApiResponse.ok(service.review(req));
    }
}
