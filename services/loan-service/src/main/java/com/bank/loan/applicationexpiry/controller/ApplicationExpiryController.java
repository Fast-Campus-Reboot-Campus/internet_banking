package com.bank.loan.applicationexpiry.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.applicationexpiry.dto.ApplicationExpiryRunResponse;
import com.bank.loan.applicationexpiry.service.ApplicationExpiryBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 승인 만료 일배치 트리거 (운영자/스케줄러용).
 * 운영에서는 매일 새벽 외부 스케줄러가 호출. 본 단계는 인증·인가 없이 노출.
 */
@Tag(name = "승인 만료", description = "ApplicationExpiry - APPROVED → EXPIRED 일배치 (internal)")
@RestController
@RequestMapping("/api/internal/application-expiry")
@RequiredArgsConstructor
@Validated
public class ApplicationExpiryController {

    private final ApplicationExpiryBatchService service;

    @Operation(summary = "승인 만료 일배치 실행",
            description = "baseDate 시점에 LoanReview.approvedAt 이 14일 이상 경과한 APPROVED 신청을 " +
                          "EXPIRED 로 전이한다. 영업일 가드 없음 (만료는 절대시점 기준).")
    @PostMapping("/run")
    public ApiResponse<ApplicationExpiryRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.run(baseDate));
    }
}
