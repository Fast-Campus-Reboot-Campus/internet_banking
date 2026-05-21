package com.bank.loan.autodebit.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.autodebit.dto.AutoDebitRunResponse;
import com.bank.loan.autodebit.service.AutoDebitBatchService;
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
 * 자동이체 배치 트리거 (운영자/스케줄러용). 실제 운영에서는 매일 새벽 외부 스케줄러가 호출.
 * 본 단계에서는 인증·인가 없이 노출 — 인증 도입 시 INTERNAL 권한 한정 필요.
 */
@Tag(name = "자동이체", description = "AutoDebit - 일배치 트리거 (internal)")
@RestController
@RequestMapping("/api/internal/auto-debit")
@RequiredArgsConstructor
@Validated
public class AutoDebitController {

    private final AutoDebitBatchService service;

    @Operation(summary = "자동이체 일배치 실행",
            description = "baseDate 의 due_date 와 일치하는 DUE 회차 중 자동이체 설정된 계약만 출금 처리한다. " +
                          "baseDate 가 비영업일(BUSINESS_CALENDAR) 이면 출금 미수행하고 skipReason=NON_BUSINESS_DAY 로 반환. " +
                          "미설정/미검증/이미 PAID 회차는 skip.")
    @PostMapping("/run")
    public ApiResponse<AutoDebitRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.run(baseDate));
    }
}
