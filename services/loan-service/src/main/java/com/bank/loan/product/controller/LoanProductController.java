package com.bank.loan.product.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.product.dto.CreateLoanProductRequest;
import com.bank.loan.product.dto.LoanProductResponse;
import com.bank.loan.product.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출상품", description = "LoanProduct - 대출 상품 관리")
@RestController
@RequestMapping("/api/loan-products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService service;

    @Operation(summary = "상품 등록", description = "신규 대출 상품을 등록한다. 초기 상태는 DRAFT.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanProductResponse>> create(@Valid @RequestBody CreateLoanProductRequest req) {
        LoanProductResponse saved = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
