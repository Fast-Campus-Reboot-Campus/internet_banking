package com.bank.loan.consent.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.consent.dto.CreateCreditConsentRequest;
import com.bank.loan.consent.dto.CreditConsentResponse;
import com.bank.loan.consent.service.CreditConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신용조회 동의", description = "CreditConsent - 신용정보 동의")
@RestController
@RequestMapping("/api/loan-applications/{applId}/credit-consents")
@RequiredArgsConstructor
public class CreditConsentController {

    private static final int DEVICE_MAX_LEN = 200;

    private final CreditConsentService service;

    @Operation(summary = "신용조회 동의",
            description = "신청자의 신용정보 조회 동의를 기록한다. 동의 즉시 consentYn=Y / withdrawnYn=N.")
    @PostMapping
    public ResponseEntity<ApiResponse<CreditConsentResponse>> create(
            @PathVariable Long applId,
            @Valid @RequestBody CreateCreditConsentRequest req,
            HttpServletRequest http) {
        CreditConsentResponse saved = service.create(
                applId, req, extractClientIp(http), extractDevice(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
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
