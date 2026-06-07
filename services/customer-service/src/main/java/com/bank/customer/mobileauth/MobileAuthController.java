package com.bank.customer.mobileauth;

import com.bank.common.web.ApiResponse;
import com.bank.customer.mobileauth.dto.SendMobileAuthRequest;
import com.bank.customer.mobileauth.dto.VerifyMobileAuthRequest;
import com.bank.customer.mobileauth.dto.VerifyMobileAuthResponse;
import com.bank.customer.mobileauth.service.MobileAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile-auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    /** 인증 코드 발송 */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Long>> send(
            @Valid @RequestBody SendMobileAuthRequest request,
            @RequestHeader(value = "X-Customer-Id", required = false) Long customerId,
            HttpServletRequest httpRequest) {
        Long authId = mobileAuthService.send(request, extractIp(httpRequest), customerId);
        return ResponseEntity.ok(ApiResponse.ok(authId));
    }

    /** 인증 코드 검증 — 신원확인 목적이면 응답에 verificationId 포함 */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyMobileAuthResponse>> verify(
            @Valid @RequestBody VerifyMobileAuthRequest request,
            @RequestHeader(value = "X-Customer-Id", required = false) Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(mobileAuthService.verify(request, customerId)));
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
