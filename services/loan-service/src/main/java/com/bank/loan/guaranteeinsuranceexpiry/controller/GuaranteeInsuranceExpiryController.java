package com.bank.loan.guaranteeinsuranceexpiry.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.guaranteeinsuranceexpiry.dto.GuaranteeInsuranceExpiryRunResponse;
import com.bank.loan.guaranteeinsuranceexpiry.service.GuaranteeInsuranceExpiryBatchService;
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
 * 보증보험 만기 일배치 트리거 (운영자/스케줄러용).
 * 운영에서는 매일 새벽 외부 스케줄러가 호출. 본 단계는 인증·인가 없이 노출.
 */
@Tag(name = "보증보험 만기", description = "GuaranteeInsuranceExpiry - ISSUED → EXPIRED 일배치 (internal)")
@RestController
@RequestMapping("/api/internal/guarantee-insurance-expiry")
@RequiredArgsConstructor
@Validated
public class GuaranteeInsuranceExpiryController {

    private final GuaranteeInsuranceExpiryBatchService service;

    @Operation(summary = "보증보험 만기 일배치 실행",
            description = "baseDate 시점에 gins_end_date 가 이미 경과한 ISSUED 보증보험을 " +
                          "EXPIRED 로 전이한다. 영업일 가드 없음(절대시점 기준).")
    @PostMapping("/run")
    public ApiResponse<GuaranteeInsuranceExpiryRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.run(baseDate));
    }
}
