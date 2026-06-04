package com.bank.customer.settings;

import com.bank.common.web.ApiResponse;
import com.bank.customer.settings.dto.ChangePasswordRequest;
import com.bank.customer.settings.dto.SettingsResponse;
import com.bank.customer.settings.dto.UpdateNotificationRequest;
import com.bank.customer.settings.dto.UpdateProfileRequest;
import com.bank.customer.settings.dto.WithdrawRequest;
import com.bank.customer.settings.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getSettings(customerId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody UpdateProfileRequest request) {
        settingsService.updateProfile(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/notification")
    public ResponseEntity<ApiResponse<Void>> updateNotification(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody UpdateNotificationRequest request) {
        settingsService.updateNotification(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        settingsService.changePassword(customerId, request, extractIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody WithdrawRequest request) {
        settingsService.withdraw(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
