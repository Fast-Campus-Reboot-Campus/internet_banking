package com.bank.loan.idv.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.idv.dto.IdentityVerificationResponse;
import com.bank.loan.idv.dto.VerifyIdentityRequest;
import com.bank.loan.idv.service.LoanIdentityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@Tag(name = "본인확인(IDV)", description = "LoanIdentityVerification - 신청자 본인확인")
@RestController
@RequestMapping("/api/loan-applications/{applId}/identity-verifications")
@RequiredArgsConstructor
public class LoanIdentityVerificationController {

    private static final int DEVICE_MAX_LEN = 200;

    private final LoanIdentityVerificationService service;

    @Operation(summary = "본인확인(IDV)",
            description = "외부 인증기관(PASS/카카오/공동인증) 연계. 본 단계에서는 stub — 항상 PASS.")
    @PostMapping
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> verify(
            @PathVariable Long applId,
            @Valid @RequestBody VerifyIdentityRequest req,
            HttpServletRequest http) {
        IdentityVerificationResponse saved = service.verify(
                applId, req, extractClientIp(http), extractDevice(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "IDV 상태 조회")
    @GetMapping("/{idvId}")
    public ApiResponse<IdentityVerificationResponse> get(
            @PathVariable Long applId, @PathVariable Long idvId) {
        return ApiResponse.ok(service.get(idvId));
    }

    private String extractClientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    private String extractDevice(HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        if (ua == null) return null;
        return ua.length() > DEVICE_MAX_LEN ? ua.substring(0, DEVICE_MAX_LEN) : ua;
    }
}
