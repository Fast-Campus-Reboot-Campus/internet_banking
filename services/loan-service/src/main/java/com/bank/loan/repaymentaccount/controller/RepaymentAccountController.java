package com.bank.loan.repaymentaccount.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.repaymentaccount.dto.RegisterRepaymentAccountRequest;
import com.bank.loan.repaymentaccount.dto.RepaymentAccountResponse;
import com.bank.loan.repaymentaccount.dto.VerifyRepaymentAccountRequest;
import com.bank.loan.repaymentaccount.service.RepaymentAccountService;
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

@Tag(name = "상환계좌", description = "RepaymentAccount - 등록·검증·조회")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/repayment-account")
@RequiredArgsConstructor
public class RepaymentAccountController {

    private final RepaymentAccountService service;

    @Operation(summary = "상환계좌 등록",
            description = "약정에 상환계좌를 등록한다. 계약당 1건만 가능. 초기 상태 REGISTERED — drawdown 전 verify 필요.")
    @PostMapping
    public ResponseEntity<ApiResponse<RepaymentAccountResponse>> register(
            @PathVariable Long cntrId,
            @Valid @RequestBody RegisterRepaymentAccountRequest req) {
        RepaymentAccountResponse saved = service.register(cntrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "상환계좌 검증",
            description = "외부 계좌검증(예금주 실명조회 등) 연계는 stub. 본 호출 자체가 성공으로 처리되어 VERIFIED 로 전이된다.")
    @PostMapping("/verify")
    public ApiResponse<RepaymentAccountResponse> verify(
            @PathVariable Long cntrId,
            @Valid @RequestBody VerifyRepaymentAccountRequest req) {
        return ApiResponse.ok(service.verify(cntrId, req));
    }

    @Operation(summary = "상환계좌 단건 조회")
    @GetMapping
    public ApiResponse<RepaymentAccountResponse> get(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.get(cntrId));
    }
}
