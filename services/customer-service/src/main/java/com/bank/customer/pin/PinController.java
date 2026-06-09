package com.bank.customer.pin;

import com.bank.common.web.ApiResponse;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.pin.dto.PinLoginRequest;
import com.bank.customer.pin.dto.RegisterPinRequest;
import com.bank.customer.pin.service.PinService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;

    /** PIN 등록 */
    @PostMapping("/customers/me/pin")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody RegisterPinRequest request) {
        pinService.register(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** PIN 로그인 */
    @PostMapping("/auth/pin-login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody PinLoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(pinService.login(request, ip, userAgent)));
    }

    /** X-Forwarded-For 우선, 없으면 RemoteAddr. 다중 IP면 첫 번째(실제 클라이언트)를 사용. */
    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** PIN 해제 */
    @DeleteMapping("/customers/me/pin")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestParam Long deviceId) {
        pinService.revoke(customerId, deviceId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
