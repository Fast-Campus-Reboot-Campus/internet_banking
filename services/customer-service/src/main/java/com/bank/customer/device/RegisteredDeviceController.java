package com.bank.customer.device;

import com.bank.common.web.ApiResponse;
import com.bank.customer.device.dto.RegisterDeviceRequest;
import com.bank.customer.device.dto.RegisteredDeviceResponse;
import com.bank.customer.device.service.RegisteredDeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/devices")
@RequiredArgsConstructor
public class RegisteredDeviceController {

    private final RegisteredDeviceService deviceService;

    @PostMapping
    public ResponseEntity<ApiResponse<RegisteredDeviceResponse>> register(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody RegisterDeviceRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(deviceService.register(customerId, request, extractIp(httpRequest))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RegisteredDeviceResponse>>> listMyDevices(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.listMyDevices(customerId)));
    }

    @PatchMapping("/{deviceId}/trust")
    public ResponseEntity<ApiResponse<RegisteredDeviceResponse>> trust(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.trust(customerId, deviceId)));
    }

    @PatchMapping("/{deviceId}/untrust")
    public ResponseEntity<ApiResponse<RegisteredDeviceResponse>> untrust(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.untrust(customerId, deviceId)));
    }

    @PatchMapping("/{deviceId}/designate")
    public ResponseEntity<ApiResponse<RegisteredDeviceResponse>> designate(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.designate(customerId, deviceId)));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long deviceId) {
        deviceService.revoke(customerId, deviceId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
