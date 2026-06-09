package com.bank.customer.cert;

import com.bank.common.web.ApiResponse;
import com.bank.customer.cert.dto.CertLoginRequest;
import com.bank.customer.cert.service.CertLoginService;
import com.bank.customer.login.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CertLoginController {

    private final CertLoginService certLoginService;

    @PostMapping("/cert-login")
    public ResponseEntity<ApiResponse<LoginResponse>> certLogin(
            @Valid @RequestBody CertLoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(certLoginService.certLogin(request, ip, userAgent)));
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
