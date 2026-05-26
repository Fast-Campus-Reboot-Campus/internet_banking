package com.bank.loan.partialrepayment.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.partialrepayment.dto.PartialRepayRequest;
import com.bank.loan.partialrepayment.dto.PartialRepaymentResponse;
import com.bank.loan.partialrepayment.service.PartialRepaymentService;
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

@Tag(name = "부분상환", description = "PartialRepayment - 회차 일부만 납부 (TYPE_PARTIAL)")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/repayments/partial")
@RequiredArgsConstructor
public class PartialRepaymentController {

    private final PartialRepaymentService service;

    @Operation(summary = "회차 부분상환",
            description = "지정 회차에 임의 금액(잔액 이하) 을 납부한다. " +
                          "납부 후 회차 누적이 예정 총액과 같아지면 자동으로 PAID, 미달이면 PARTIAL_PAID 로 전이. " +
                          "비례 분배(원금/이자) 적용. Idempotency-Key 헤더 권장.")
    @PostMapping
    public ResponseEntity<ApiResponse<PartialRepaymentResponse>> repay(
            @PathVariable Long cntrId,
            @Valid @RequestBody PartialRepayRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PartialRepaymentResponse result = service.repay(cntrId, req, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }
}
