package com.bank.loan.accrual.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.accrual.dto.InterestAccrualRunResponse;
import com.bank.loan.accrual.service.InterestAccrualBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이자발생", description = "InterestAccrual - 일배치 트리거 (internal)")
@RestController
@RequestMapping("/api/internal/interest-accrual")
@RequiredArgsConstructor
@Validated
public class InterestAccrualBatchController {

    private final InterestAccrualBatchService service;

    @Operation(summary = "일별 이자 발생 배치",
            description = "ACTIVE 계약 전체에 baseDate 의 일이자를 발생·누적한다. " +
                          "잔액 0 / 이미 적재된 행은 skip. UNIQUE(cntr_id, accrual_date) 멱등.")
    @PostMapping("/run")
    public ApiResponse<InterestAccrualRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.run(baseDate));
    }
}
