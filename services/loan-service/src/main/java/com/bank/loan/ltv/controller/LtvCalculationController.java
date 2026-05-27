package com.bank.loan.ltv.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.ltv.dto.LtvCalculationResponse;
import com.bank.loan.ltv.dto.RunLtvCalculationRequest;
import com.bank.loan.ltv.service.LtvCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LTV 산정", description = "LtvCalculation - 담보별 LTV 한도 산정 + 조회")
@RestController
@RequestMapping("/api/collaterals/{colId}/ltv-calculation")
@RequiredArgsConstructor
public class LtvCalculationController {

    private final LtvCalculationService service;

    @Operation(summary = "LTV 산정 실행",
            description = "사전조건: 담보 감정평가 DONE 1건 이상. 담보당 1건 (col_id UNIQUE 운영). " +
                          "max_loan = applied_col_value × limit_bps/10000 - senior_lien (음수 시 0). " +
                          "ratio_bps = requested_amount / applied_col_value × 10000. " +
                          "requested ≤ max_loan 이면 PASS, 초과 시 FAIL. " +
                          "기본 한도 7000bps(70%) — 주택담보 보편치.")
    @PostMapping
    public ResponseEntity<ApiResponse<LtvCalculationResponse>> run(
            @PathVariable Long colId,
            @Valid @RequestBody RunLtvCalculationRequest req) {
        LtvCalculationResponse saved = service.run(colId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "LTV 산정 결과 조회")
    @GetMapping
    public ApiResponse<LtvCalculationResponse> get(@PathVariable Long colId) {
        return ApiResponse.ok(service.get(colId));
    }
}
