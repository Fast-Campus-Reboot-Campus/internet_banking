package com.bank.loan.contract.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.contract.dto.CreateContractRequest;
import com.bank.loan.contract.dto.LoanContractResponse;
import com.bank.loan.contract.service.LoanContractService;
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

@Tag(name = "대출계약", description = "LoanContract - 약정한도 설정 및 조회")
@RestController
@RequestMapping("/api/loan-contracts")
@RequiredArgsConstructor
public class LoanContractController {

    private final LoanContractService service;

    @Operation(summary = "약정한도 설정",
            description = "승인된 신청에 대해 최종 약정 한도·금리·기간을 확정한다. 초기 상태 SIGNED.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanContractResponse>> create(
            @Valid @RequestBody CreateContractRequest req) {
        LoanContractResponse saved = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "대출 계약 단건 조회")
    @GetMapping("/{cntrId}")
    public ApiResponse<LoanContractResponse> get(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.get(cntrId));
    }
}
