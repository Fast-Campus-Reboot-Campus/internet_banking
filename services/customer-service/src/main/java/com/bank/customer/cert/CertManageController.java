package com.bank.customer.cert;

import com.bank.common.web.ApiResponse;
import com.bank.customer.cert.dto.CertDetailResponse;
import com.bank.customer.cert.dto.CertPinChangeRequest;
import com.bank.customer.cert.dto.CertSummaryResponse;
import com.bank.customer.cert.service.CertManageService;
import com.bank.customer.cert.service.CertPinChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cert/manage")
@RequiredArgsConstructor
public class CertManageController {

    private final CertManageService certManageService;
    private final CertPinChangeService certPinChangeService;

    /** 내 인증서 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CertSummaryResponse>>> list(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(certManageService.listCerts(customerId)));
    }

    /** 인증서 상세 조회 */
    @GetMapping("/{serialNumber}")
    public ResponseEntity<ApiResponse<CertDetailResponse>> detail(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable String serialNumber) {
        return ResponseEntity.ok(ApiResponse.ok(certManageService.getCertDetail(customerId, serialNumber)));
    }

    /** 인증서 폐기 */
    @DeleteMapping("/{serialNumber}")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable String serialNumber) {
        certManageService.revoke(customerId, serialNumber);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /** 인증서 암호 변경 */
    @PutMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> changePin(
            @Valid @RequestBody CertPinChangeRequest request) {
        certPinChangeService.changePin(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
