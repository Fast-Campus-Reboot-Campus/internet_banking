package com.bank.customer.register;

import com.bank.common.web.ApiResponse;
import com.bank.customer.register.dto.CorporateRegisterRequest;
import com.bank.customer.register.dto.RegisterRequest;
import com.bank.customer.register.dto.RegisterResponse;
import com.bank.customer.register.service.CorporateRegisterService;
import com.bank.customer.register.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService          registerService;
    private final CorporateRegisterService corporateRegisterService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(registerService.register(request)));
    }

    @PostMapping("/register/corporate")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerCorporate(
            @Valid @RequestBody CorporateRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(corporateRegisterService.register(request)));
    }
}
