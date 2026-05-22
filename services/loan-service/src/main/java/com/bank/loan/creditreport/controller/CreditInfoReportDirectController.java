package com.bank.loan.creditreport.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.creditreport.dto.AckCallbackRequest;
import com.bank.loan.creditreport.dto.CreditInfoReportResponse;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @Operation(summary = "운영자 재전송",
            description = "DEAD/FAILED 신고를 강제 재시도. outbox attemptNo 리셋 + PENDING 복귀. " +
                          "ACKED 는 LOAN_152.")
    @PostMapping("/{crptId}/retry")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreditInfoReportResponse> retry(@PathVariable Long crptId) {
        return ApiResponse.ok(service.retry(crptId));
    }

    @Operation(summary = "외부 기관 ACK callback",
            description = "신고 SENT → ACKED 전이. 이미 ACKED 면 동일 row 반환(멱등). " +
                          "SENT 이외 상태는 LOAN_151. 인증/서명검증은 11 plan 에서 보강 예정.")
    @PostMapping("/{crptId}/ack")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreditInfoReportResponse> ack(
            @PathVariable Long crptId,
            @Valid @RequestBody AckCallbackRequest req) {
        return ApiResponse.ok(service.ack(crptId, req));
    }
}
