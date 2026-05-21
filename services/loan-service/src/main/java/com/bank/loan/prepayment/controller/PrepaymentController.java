package com.bank.loan.prepayment.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.prepayment.dto.PrepayRequest;
import com.bank.loan.prepayment.dto.PrepaymentResponse;
import com.bank.loan.prepayment.service.PrepaymentService;
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

@Tag(name = "중도상환", description = "Prepayment - 잔여 원금 일부/전액 조기 상환 (TYPE_EARLY)")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/prepayments")
@RequiredArgsConstructor
public class PrepaymentController {

    private final PrepaymentService service;

    @Operation(summary = "중도상환 실행",
            description = "잔여 원금 한도 내에서 임의 금액을 즉시 상환한다. " +
                          "처리 후 최신 버전의 DUE/OVERDUE 회차는 SUPERSEDED 로 전이되고, " +
                          "잔액이 남아있으면 새 버전(V{n+1}) 으로 같은 회차 수만큼 재계산해 발급한다. " +
                          "Idempotency-Key 헤더 권장. 미발생이자·수수료는 본 단계 0 으로 처리.")
    @PostMapping
    public ResponseEntity<ApiResponse<PrepaymentResponse>> prepay(
            @PathVariable Long cntrId,
            @Valid @RequestBody PrepayRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PrepaymentResponse result = service.prepay(cntrId, req, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }
}
