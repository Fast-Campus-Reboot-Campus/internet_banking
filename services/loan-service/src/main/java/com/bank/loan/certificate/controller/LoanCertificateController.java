package com.bank.loan.certificate.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.certificate.dto.IssueCertificateRequest;
import com.bank.loan.certificate.dto.LoanCertificateListResponse;
import com.bank.loan.certificate.dto.LoanCertificateResponse;
import com.bank.loan.certificate.service.LoanCertificateService;
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

@Tag(name = "증명서", description = "LoanCertificate - 발급·이력 (계약 경로)")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/certificates")
@RequiredArgsConstructor
public class LoanCertificateController {

    private final LoanCertificateService service;

    @Operation(summary = "증명서 발급",
            description = "BALANCE/DEBT/REPAYMENT 등 유형별 즉시 발급. cert_no 자동 채번. " +
                          "다발 발급 허용 — 재발급은 새 row 로 누적된다.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanCertificateResponse>> issue(
            @PathVariable Long cntrId,
            @Valid @RequestBody IssueCertificateRequest req) {
        LoanCertificateResponse saved = service.issue(cntrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "증명서 발급 이력",
            description = "계약의 모든 증명서를 issued_at 오름차순으로 반환.")
    @GetMapping
    public ApiResponse<LoanCertificateListResponse> list(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.list(cntrId));
    }
}
