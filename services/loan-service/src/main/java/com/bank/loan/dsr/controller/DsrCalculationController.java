package com.bank.loan.dsr.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.dsr.dto.DsrCalculationResponse;
import com.bank.loan.dsr.dto.RunDsrCalculationRequest;
import com.bank.loan.dsr.service.DsrCalculationService;
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

@Tag(name = "DSR/한도 산정", description = "DsrCalculation - 신청별 DSR 한도 산정 + 조회")
@RestController
@RequestMapping("/api/loan-applications/{applId}/dsr-calculation")
@RequiredArgsConstructor
public class DsrCalculationController {

    private final DsrCalculationService service;

    @Operation(summary = "DSR 산정 실행",
            description = "사전조건: 신용평가 완료. 신청당 1건 (appl_id UNIQUE). " +
                          "new_annual_repay_amt 미지정 시 상품 baseRate 기반 단순 추정. " +
                          "ratio_bps ≤ limit_bps 면 PASS, 초과 시 FAIL. " +
                          "신청 상태는 본 단계에서 전이하지 않으며 본심사에서 종합한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<DsrCalculationResponse>> run(
            @PathVariable Long applId,
            @Valid @RequestBody RunDsrCalculationRequest req) {
        DsrCalculationResponse saved = service.run(applId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "DSR 산정 결과 조회")
    @GetMapping
    public ApiResponse<DsrCalculationResponse> get(@PathVariable Long applId) {
        return ApiResponse.ok(service.get(applId));
    }
}
