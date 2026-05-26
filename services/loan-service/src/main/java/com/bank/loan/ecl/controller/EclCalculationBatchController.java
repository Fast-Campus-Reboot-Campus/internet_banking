package com.bank.loan.ecl.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.ecl.dto.EclCalculationRunResponse;
import com.bank.loan.ecl.service.EclCalculationBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IFRS9 ECL", description = "LoanEclSummary - 월별 ECL 산출 (internal)")
@RestController
@RequestMapping("/api/internal/ecl")
@RequiredArgsConstructor
@Validated
public class EclCalculationBatchController {

    private final EclCalculationBatchService service;

    @Operation(summary = "IFRS9 ECL 월별 산출",
            description = "baseMonth 의 ACTIVE 약정 전체에 대해 PD/LGD/EAD 기반 ECL 을 산출·적재한다. " +
                          "UNIQUE(cntr_id, summary_month) 멱등 — 약정별로 이미 적재된 row 는 skip.")
    @PostMapping("/run")
    public ApiResponse<EclCalculationRunResponse> run(
            @RequestParam("baseMonth") @Pattern(regexp = "\\d{6}") String baseMonth) {
        return ApiResponse.ok(service.run(baseMonth));
    }
}
