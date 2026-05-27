package com.bank.loan.review.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.review.dto.ExpirePendingReviewsResponse;
import com.bank.loan.review.service.LoanReviewAutoDecideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "본심사 배치", description = "InternalReviewBatch - 본심사 관련 운영 배치 endpoint")
@RestController
@RequestMapping("/api/internal/loan-reviews")
@RequiredArgsConstructor
@Validated
public class InternalReviewBatchController {

    private final LoanReviewAutoDecideService service;

    @Operation(summary = "권고 만료 배치",
            description = "reviewedAt 이 olderThanDays 일 이전인 PENDING_APPROVAL 본심사를 일괄 EXPIRED 로 전이. "
                    + "기본값 7일. 만료된 권고는 신청 상태에 영향 없음(PRESCREENED 유지) — "
                    + "운영자가 별도 수동 본심사 등으로 처리.")
    @PostMapping("/expire-pending")
    public ApiResponse<ExpirePendingReviewsResponse> expirePending(
            @RequestParam(defaultValue = "7") @Min(0) int olderThanDays) {
        return ApiResponse.ok(service.expirePending(olderThanDays));
    }
}
