package com.bank.loan.accounting.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.accounting.dto.AccountingSummaryRunResponse;
import com.bank.loan.accounting.service.AccountingSummaryBatchService;
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
 * 일일 회계 요약 배치 트리거 (internal).
 * 운영에서는 EOD 잡 (LoanEodJob) 안의 accountingSummaryStep 으로 자동 호출. 본 엔드포인트는 수동 재처리용.
 */
@Tag(name = "회계 요약", description = "DailyAccountingSummary - 일일 정산용 합계 (internal)")
@RestController
@RequestMapping("/api/internal/accounting-summary")
@RequiredArgsConstructor
@Validated
public class AccountingSummaryBatchController {

    private final AccountingSummaryBatchService service;

    @Operation(summary = "일일 회계 요약 산출",
            description = "baseDate 의 이자/연체이자/자동이체/실행 합계 + ACTIVE 약정·연체 카운트를 적재한다. " +
                          "UNIQUE(summary_date) 로 멱등 — 동일 baseDate 재호출 시 skip.")
    @PostMapping("/run")
    public ApiResponse<AccountingSummaryRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.run(baseDate));
    }
}
