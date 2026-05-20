package com.bank.loan.execution.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.execution.dto.DrawdownRequest;
import com.bank.loan.execution.dto.LoanExecutionResponse;
import com.bank.loan.execution.service.LoanExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출실행", description = "LoanExecution - 자금 인출 (Drawdown)")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/executions")
@RequiredArgsConstructor
public class LoanExecutionController {

    private final LoanExecutionService service;

    @Operation(summary = "자금 인출 (Drawdown)",
            description = "약정한도 범위 내에서 자금을 지급. Idempotency-Key 헤더로 중복 호출 방어. 최초 인출 시 계약 ACTIVE 로 전이.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanExecutionResponse>> drawdown(
            @PathVariable Long cntrId,
            @Valid @RequestBody DrawdownRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        LoanExecutionResponse saved = service.drawdown(cntrId, req, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
