package com.bank.customer.login;

import com.bank.common.web.ApiResponse;
import com.bank.customer.login.dto.LoginRequest;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.login.dto.RefreshRequest;
import com.bank.customer.login.service.LoginService;
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
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(loginService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(loginService.refresh(request)));
    }
}
