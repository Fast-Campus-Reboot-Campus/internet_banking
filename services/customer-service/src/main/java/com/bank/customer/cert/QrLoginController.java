package com.bank.customer.cert;

import com.bank.common.web.ApiResponse;
import com.bank.customer.cert.dto.QrApproveRequest;
import com.bank.customer.cert.dto.QrGenerateResponse;
import com.bank.customer.cert.dto.QrStatusResponse;
import com.bank.customer.cert.service.QrLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/qr")
@RequiredArgsConstructor
public class QrLoginController {

    private final QrLoginService qrLoginService;

    /** PC: QR 토큰 생성 */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrGenerateResponse>> generate(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.ok(qrLoginService.generate(ip)));
    }

    /** PC: 상태 폴링. APPROVED면 accessToken·refreshToken 포함 */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QrStatusResponse>> status(
            @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.ok(qrLoginService.getStatus(token)));
    }

    /** 모바일: QR 승인 (loginId + password로 인증 후 QR 토큰 APPROVED 전환) */
    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @Valid @RequestBody QrApproveRequest request,
            HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        String userAgent = req.getHeader("User-Agent");
        qrLoginService.approve(request, ip, userAgent);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
