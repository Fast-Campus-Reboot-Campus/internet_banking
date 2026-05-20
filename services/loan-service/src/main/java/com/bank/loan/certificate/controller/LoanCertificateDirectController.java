package com.bank.loan.certificate.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.certificate.dto.LoanCertificateResponse;
import com.bank.loan.certificate.service.LoanCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 증명서 ID 기반 직접 접근. 계약 경로 없이 certId 단건 조회.
 */
@Tag(name = "증명서", description = "LoanCertificate - 직접 접근")
@RestController
@RequestMapping("/api/loan-certificates")
@RequiredArgsConstructor
public class LoanCertificateDirectController {

    private final LoanCertificateService service;

    @Operation(summary = "증명서 단건 조회")
    @GetMapping("/{certId}")
    public ApiResponse<LoanCertificateResponse> get(@PathVariable Long certId) {
        return ApiResponse.ok(service.getById(certId));
    }
}
