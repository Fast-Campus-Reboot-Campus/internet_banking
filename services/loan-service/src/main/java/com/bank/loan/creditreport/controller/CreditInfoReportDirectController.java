package com.bank.loan.creditreport.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.creditreport.dto.CreditInfoReportResponse;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 ID 기반 직접 접근. 계약 경로 없이 crptId 단건 조회.
 */
@Tag(name = "신용정보신고", description = "CreditInfoReport - 직접 접근")
@RestController
@RequestMapping("/api/credit-info-reports")
@RequiredArgsConstructor
public class CreditInfoReportDirectController {

    private final CreditInfoReportService service;

    @Operation(summary = "신고 단건 조회")
    @GetMapping("/{crptId}")
    public ApiResponse<CreditInfoReportResponse> get(@PathVariable Long crptId) {
        return ApiResponse.ok(service.getById(crptId));
    }
}
