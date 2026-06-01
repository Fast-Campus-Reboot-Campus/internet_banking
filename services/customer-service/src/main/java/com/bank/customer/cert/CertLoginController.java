package com.bank.customer.cert;

import com.bank.common.web.ApiResponse;
import com.bank.customer.cert.dto.CertLoginRequest;
import com.bank.customer.cert.service.CertLoginService;
import com.bank.customer.login.dto.LoginResponse;
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
            @Valid @RequestBody CertLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(certLoginService.certLogin(request)));
    }
}
