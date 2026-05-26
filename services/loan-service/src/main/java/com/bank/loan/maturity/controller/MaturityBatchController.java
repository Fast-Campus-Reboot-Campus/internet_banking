package com.bank.loan.maturity.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.maturity.dto.MaturityRunResponse;
import com.bank.loan.maturity.service.MaturityBatchService;
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
 * 만기 도래 일배치 트리거 (internal).
 * 운영에서는 EOD 잡 (LoanEodJob) 안의 maturityStep 으로 자동 호출. 본 엔드포인트는 수동 재처리용.
 */
@Tag(name = "만기 도래", description = "Maturity - ACTIVE → MATURED 일배치 (internal)")
@RestController
@RequestMapping("/api/internal/maturity")
@RequiredArgsConstructor
@Validated
public class MaturityBatchController {

    private final MaturityBatchService service;

    @Operation(summary = "만기 도래 일배치 실행",
            description = "baseDate 시점에 current_maturity_date 가 도달한 ACTIVE 만기를 " +
                          "MATURED 로 전이한다. 영업일 가드 없음(절대시점 기준).")
    @PostMapping("/run")
    public ApiResponse<MaturityRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.run(baseDate));
    }
}
