package com.bank.loan.autodebit.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.autodebit.dto.AutoDebitPaymentResultRequest;
import com.bank.loan.autodebit.service.AutoDebitCallbackService;
import com.bank.loan.repayment.dto.RepaymentTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * payment-service → loan-service CLEARING 완결 콜백 수신.
 *
 * payment-service 가 KFTC/BOK 정산 결과를 수신한 뒤 이 엔드포인트를 호출한다.
 * X-Internal-Token 헤더로 보호 (SecurityConfig — ROLE_INTERNAL).
 *
 * POST /api/internal/auto-debit/payment-result
 *   Body: { piId, idempotencyKey, status("COMPLETED"|"FAILED"), failureCategory }
 */
@Tag(name = "자동이체", description = "AutoDebit - CLEARING 콜백 수신 (internal)")
@RestController
@RequestMapping("/api/internal/auto-debit")
@RequiredArgsConstructor
public class AutoDebitCallbackController {

    private final AutoDebitCallbackService callbackService;

    @Operation(summary = "타행 자동이체 CLEARING 완결 콜백",
            description = "payment-service 가 KFTC/BOK 정산 완료(COMPLETED) 또는 거절(FAILED) 통보 후 호출. " +
                          "idempotencyKey('AUTO-{cntrId}-{rschId}-{baseDate}') 를 파싱해 상환 처리를 완결하거나 FAILED 거래를 기록한다.")
    @PostMapping("/payment-result")
    public ResponseEntity<ApiResponse<RepaymentTransactionResponse>> paymentResult(
            @Valid @RequestBody AutoDebitPaymentResultRequest req) {
        RepaymentTransactionResponse result = callbackService.handleResult(req);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
