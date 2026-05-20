package com.bank.loan.prescreening.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.prescreening.dto.LoanPrescreeningResponse;
import com.bank.loan.prescreening.dto.RunPrescreeningRequest;
import com.bank.loan.prescreening.service.LoanPrescreeningService;
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

@Tag(name = "가심사", description = "LoanPrescreening - 한도조회/가심사 실행 + 조회")
@RestController
@RequestMapping("/api/loan-applications/{applId}/prescreening")
@RequiredArgsConstructor
public class LoanPrescreeningController {

    private final LoanPrescreeningService service;

    @Operation(summary = "가심사 실행",
            description = "외부 가심사 엔진 stub — PASS/REJECT 는 클라이언트 입력. " +
                          "PASS 시 estimated_limit/rate 자동 채움(미지정 시 requested/baseRate). " +
                          "신청 상태: SUBMITTED → PRESCREENED/REJECTED. 신청당 1건 (appl_id UNIQUE).")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanPrescreeningResponse>> run(
            @PathVariable Long applId,
            @Valid @RequestBody RunPrescreeningRequest req) {
        LoanPrescreeningResponse saved = service.run(applId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "가심사 결과 조회")
    @GetMapping
    public ApiResponse<LoanPrescreeningResponse> get(@PathVariable Long applId) {
        return ApiResponse.ok(service.get(applId));
    }
}
