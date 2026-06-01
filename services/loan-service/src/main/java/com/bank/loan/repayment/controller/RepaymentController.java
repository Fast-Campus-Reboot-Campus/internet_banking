package com.bank.loan.repayment.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.repayment.dto.RepayInstallmentRequest;
import com.bank.loan.repayment.dto.RepaymentTransactionListResponse;
import com.bank.loan.repayment.dto.RepaymentTransactionResponse;
import com.bank.loan.repayment.service.OnlineRepaymentService;
import com.bank.loan.repayment.service.RepaymentService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상환", description = "Repayment - 회차 정확액 수동 상환")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/repayments")
@RequiredArgsConstructor
public class RepaymentController {

    private final RepaymentService service;
    private final OnlineRepaymentService onlineRepaymentService;

    @Operation(summary = "회차 상환",
            description = "지정된 회차(installmentNo)의 예정 금액을 정확히 상환한다. Idempotency-Key 헤더 권장. " +
                          "중도상환은 POST /api/loan-contracts/{cntrId}/prepayments, " +
                          "부분상환은 POST /api/loan-contracts/{cntrId}/repayments/partial, " +
                          "역분개는 POST /api/loan-contracts/{cntrId}/repayments/{rtxId}/reversal 사용.")
    @PostMapping
    public ResponseEntity<ApiResponse<RepaymentTransactionResponse>> repay(
            @PathVariable Long cntrId,
            @Valid @RequestBody RepayInstallmentRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        RepaymentTransactionResponse saved = service.repayInstallment(cntrId, req, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "온라인 회차 상환",
            description = "WEB·MOBILE 채널에서 고객이 직접 상환 — payment-service 를 통해 이체 후 원장 기록. " +
                          "Idempotency-Key 헤더 필수(금융 거래 중복 방지). " +
                          "창구 수납 기록은 POST /repayments 사용.")
    @PostMapping("/online")
    public ResponseEntity<ApiResponse<RepaymentTransactionResponse>> repayOnline(
            @PathVariable Long cntrId,
            @Valid @RequestBody RepayInstallmentRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        RepaymentTransactionResponse saved =
                onlineRepaymentService.repayOnline(cntrId, req, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "상환 거래 목록", description = "계약의 모든 상환 거래를 paid_at 오름차순으로 반환한다.")
    @GetMapping
    public ApiResponse<RepaymentTransactionListResponse> list(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.list(cntrId));
    }
}
