package com.bank.loan.application.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.application.dto.CreateLoanApplicationRequest;
import com.bank.loan.application.dto.LoanApplicationResponse;
import com.bank.loan.application.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출신청", description = "LoanApplication - 대출 신청")
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private static final int DEVICE_MAX_LEN = 200;

    private final LoanApplicationService service;

    @Operation(summary = "대출 신청",
            description = "본 신청 접수. Idempotency-Key 헤더로 중복 호출 방어. 상태 SUBMITTED.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> create(
            @Valid @RequestBody CreateLoanApplicationRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest http) {

        LoanApplicationResponse saved = service.create(
                req, idempotencyKey, extractClientIp(http), extractDevice(http));
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
