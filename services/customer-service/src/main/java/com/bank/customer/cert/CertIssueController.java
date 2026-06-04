package com.bank.customer.cert;

import com.bank.common.web.ApiResponse;
import com.bank.customer.cert.dto.CertIssueRequest;
import com.bank.customer.cert.dto.CertIssueResponse;
import com.bank.customer.cert.dto.CertPinChangeRequest;
import com.bank.customer.cert.service.CertIssueService;
import com.bank.customer.cert.service.CertPinChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CertIssueController {

    private final CertIssueService certIssueService;
    private final CertPinChangeService certPinChangeService;

    @PostMapping("/cert/issue")
    public ResponseEntity<ApiResponse<CertIssueResponse>> issue(
            @Valid @RequestBody CertIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(certIssueService.issue(request)));
    }

    @PutMapping("/cert/pin")
    public ResponseEntity<ApiResponse<Void>> changePin(
            @Valid @RequestBody CertPinChangeRequest request) {
        certPinChangeService.changePin(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
