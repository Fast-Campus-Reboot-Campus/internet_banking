package com.bank.loan.creditreport.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.creditreport.dto.CreditInfoReportListResponse;
import com.bank.loan.creditreport.dto.CreditInfoReportResponse;
import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신용정보신고", description = "CreditInfoReport - KCB/NICE 신고·이력 (계약 경로)")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/credit-info-reports")
@RequiredArgsConstructor
public class CreditInfoReportController {

    private final CreditInfoReportService service;

    @Operation(summary = "신용정보 신고",
            description = "유형(NEW_LOAN/DELINQUENCY/RESOLUTION/CLOSURE)·기관(KCB/NICE) 별 신고 등록. " +
                          "본 단계는 등록 → 즉시 외부 전송(stub) → SENT 한 트랜잭션 안에서 전이. " +
                          "external_tx_no 자동 채번.")
    @PostMapping
    public ResponseEntity<ApiResponse<CreditInfoReportResponse>> submit(
            @PathVariable Long cntrId,
            @Valid @RequestBody SubmitReportRequest req) {
        CreditInfoReportResponse saved = service.submit(cntrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "신고 이력 목록",
            description = "계약의 모든 신용정보 신고를 created_at 오름차순으로 반환.")
    @GetMapping
    public ApiResponse<CreditInfoReportListResponse> list(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.list(cntrId));
    }
}
