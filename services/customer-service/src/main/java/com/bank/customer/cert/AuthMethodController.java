package com.bank.customer.cert;

import com.bank.common.web.ApiResponse;
import com.bank.customer.cert.dto.AuthMethodResponse;
import com.bank.customer.cert.service.AuthMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/auth-methods")
@RequiredArgsConstructor
public class AuthMethodController {

    private final AuthMethodService authMethodService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthMethodResponse>>> list(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(authMethodService.listMyAuthMethods(customerId)));
    }

    @PatchMapping("/{authMethodId}/alias")
    public ResponseEntity<ApiResponse<AuthMethodResponse>> updateAlias(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long authMethodId,
            @RequestParam String alias) {
        return ResponseEntity.ok(ApiResponse.ok(authMethodService.updateAlias(customerId, authMethodId, alias)));
    }

    @PatchMapping("/{authMethodId}/primary")
    public ResponseEntity<ApiResponse<AuthMethodResponse>> setPrimary(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long authMethodId) {
        return ResponseEntity.ok(ApiResponse.ok(authMethodService.setPrimary(customerId, authMethodId)));
    }

    @DeleteMapping("/{authMethodId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long authMethodId) {
        authMethodService.deactivate(customerId, authMethodId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
