package com.bank.loan.product.preferential.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.product.preferential.dto.CreatePreferentialRatePolicyRequest;
import com.bank.loan.product.preferential.dto.PreferentialRatePolicyResponse;
import com.bank.loan.product.preferential.service.PreferentialRatePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출상품 - 우대금리", description = "PreferentialRatePolicy - 상품별 우대금리 정책")
@RestController
@RequestMapping("/api/loan-products/{prodId}/preferential-rate-policies")
@RequiredArgsConstructor
public class PreferentialRatePolicyController {

    private final PreferentialRatePolicyService service;

    @Operation(summary = "우대금리 정책 등록",
            description = "상품에 우대금리 정책을 등록한다. 초기 active_yn=Y, 동일 조건의 활성 정책은 중복 불가.")
    @PostMapping
    public ResponseEntity<ApiResponse<PreferentialRatePolicyResponse>> create(
            @PathVariable Long prodId,
            @Valid @RequestBody CreatePreferentialRatePolicyRequest req) {
        PreferentialRatePolicyResponse saved = service.create(prodId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
