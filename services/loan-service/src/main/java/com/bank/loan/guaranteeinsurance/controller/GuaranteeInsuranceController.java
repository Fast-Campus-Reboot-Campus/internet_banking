package com.bank.loan.guaranteeinsurance.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.guaranteeinsurance.dto.CancelGuaranteeInsuranceRequest;
import com.bank.loan.guaranteeinsurance.dto.GuaranteeInsuranceResponse;
import com.bank.loan.guaranteeinsurance.dto.IssueGuaranteeInsuranceRequest;
import com.bank.loan.guaranteeinsurance.service.GuaranteeInsuranceService;
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

@Tag(name = "보증보험", description = "GuaranteeInsurance - 외부 보증기관(SGI/HUG/HF) 발급/취소/조회")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/guarantee-insurance")
@RequiredArgsConstructor
public class GuaranteeInsuranceController {

    private final GuaranteeInsuranceService service;

    @Operation(summary = "보증보험 발급",
            description = "외부기관 stub — request 즉시 ISSUED 처리. 발급 가능 계약 상태 SIGNED/ACTIVE. " +
                          "계약당 활성(ISSUED) 1건 차단. 시작/종료일 미지정 시 계약 기간 자동 적용.")
    @PostMapping
    public ResponseEntity<ApiResponse<GuaranteeInsuranceResponse>> issue(
            @PathVariable Long cntrId,
            @Valid @RequestBody IssueGuaranteeInsuranceRequest req) {
        GuaranteeInsuranceResponse saved = service.issue(cntrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "보증보험 취소", description = "ISSUED → CANCELED. 다른 상태는 차단.")
    @PostMapping("/{ginsId}/cancel")
    public ApiResponse<GuaranteeInsuranceResponse> cancel(
            @PathVariable Long cntrId,
            @PathVariable Long ginsId,
            @RequestBody(required = false) @Valid CancelGuaranteeInsuranceRequest req) {
        return ApiResponse.ok(service.cancel(cntrId, ginsId, req));
    }

    @Operation(summary = "보증보험 단건 조회")
    @GetMapping("/{ginsId}")
    public ApiResponse<GuaranteeInsuranceResponse> get(
            @PathVariable Long cntrId,
            @PathVariable Long ginsId) {
        return ApiResponse.ok(service.get(cntrId, ginsId));
    }
}
