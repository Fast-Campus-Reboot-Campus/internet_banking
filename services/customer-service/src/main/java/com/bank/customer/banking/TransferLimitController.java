package com.bank.customer.banking;

import com.bank.common.web.ApiResponse;
import com.bank.customer.banking.dto.ReduceTransferLimitRequest;
import com.bank.customer.banking.dto.TransferLimitResponse;
import com.bank.customer.banking.service.TransferLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 고객당 인터넷뱅킹 이체한도 조회/감액. (계좌당 출금한도는 deposit-service 소관) */
@RestController
@RequestMapping("/api/v1/customers/me/transfer-limit")
@RequiredArgsConstructor
public class TransferLimitController {

    private final TransferLimitService transferLimitService;

    @GetMapping
    public ResponseEntity<ApiResponse<TransferLimitResponse>> getLimit(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(transferLimitService.getLimit(customerId)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> reduce(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody ReduceTransferLimitRequest request) {
        transferLimitService.reduce(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
