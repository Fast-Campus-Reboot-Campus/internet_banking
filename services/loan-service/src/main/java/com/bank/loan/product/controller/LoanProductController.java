package com.bank.loan.product.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.product.dto.CreateLoanProductRequest;
import com.bank.loan.product.dto.DiscontinueLoanProductRequest;
import com.bank.loan.product.dto.LoanProductListResponse;
import com.bank.loan.product.dto.LoanProductResponse;
import com.bank.loan.product.dto.UpdateLoanProductRequest;
import com.bank.loan.product.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출상품", description = "LoanProduct - 대출 상품 관리")
@RestController
@RequestMapping("/api/loan-products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService service;

    @Operation(summary = "상품 목록", description = "loanTypeCd / prodStatusCd 필터 + 페이지네이션으로 상품 목록을 조회한다.")
    @GetMapping
    public ApiResponse<LoanProductListResponse> list(
            @RequestParam(required = false) String loanTypeCd,
            @RequestParam(required = false) String prodStatusCd,
            Pageable pageable) {
        return ApiResponse.ok(service.list(loanTypeCd, prodStatusCd, pageable));
    }

    @Operation(summary = "상품 단건 조회", description = "prodId 로 대출 상품 단건을 조회한다.")
    @GetMapping("/{prodId}")
    public ApiResponse<LoanProductResponse> get(@PathVariable Long prodId) {
        return ApiResponse.ok(service.get(prodId));
    }

    @Operation(summary = "상품 등록", description = "신규 대출 상품을 등록한다. 초기 상태는 DRAFT.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanProductResponse>> create(@Valid @RequestBody CreateLoanProductRequest req) {
        LoanProductResponse saved = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "상품 수정", description = "지정된 필드만 부분 수정한다. prodCd 는 변경 불가.")
    @PatchMapping("/{prodId}")
    public ApiResponse<LoanProductResponse> update(@PathVariable Long prodId,
                                                   @Valid @RequestBody UpdateLoanProductRequest req) {
        return ApiResponse.ok(service.update(prodId, req));
    }

    @Operation(summary = "상품 단종",
            description = "판매 종료일과 사유를 받아 상태를 DISCONTINUED 로 전환한다. status_history 에 이력 기록.")
    @PostMapping("/{prodId}/discontinue")
    public ApiResponse<LoanProductResponse> discontinue(@PathVariable Long prodId,
                                                        @Valid @RequestBody DiscontinueLoanProductRequest req) {
        return ApiResponse.ok(service.discontinue(prodId, req));
    }
}
