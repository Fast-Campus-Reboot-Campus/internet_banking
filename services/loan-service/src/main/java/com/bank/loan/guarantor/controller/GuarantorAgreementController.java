package com.bank.loan.guarantor.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.guarantor.dto.CancelGuarantorAgreementRequest;
import com.bank.loan.guarantor.dto.GuarantorAgreementListResponse;
import com.bank.loan.guarantor.dto.GuarantorAgreementResponse;
import com.bank.loan.guarantor.dto.RegisterGuarantorAgreementRequest;
import com.bank.loan.guarantor.dto.SignGuarantorAgreementRequest;
import com.bank.loan.guarantor.service.GuarantorAgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "보증 약정", description = "GuarantorAgreement - 보증인 약정 등록/서명/취소 (flows §1.1)")
@RestController
@RequestMapping("/api/loan-applications/{applId}/guarantor-agreements")
@RequiredArgsConstructor
public class GuarantorAgreementController {

    private final GuarantorAgreementService service;

    @Operation(summary = "보증 약정 등록",
            description = "보증인 정보(name/mobile/relation) + 약정 정보(type/amount/ratio) 를 함께 받아 " +
                          "GuarantorMaster 와 GuarantorAgreement 를 생성한다. 등록 가능한 신청 상태: " +
                          "SUBMITTED/PRESCREENED/REVIEWING/APPROVED. 동일 보증인 중복 등록 차단.")
    @PostMapping
    public ResponseEntity<ApiResponse<GuarantorAgreementResponse>> register(
            @PathVariable Long applId,
            @Valid @RequestBody RegisterGuarantorAgreementRequest req) {
        GuarantorAgreementResponse saved = service.register(applId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "보증 약정 전자서명 완료",
            description = "REGISTERED → SIGNED. 서명 문서 URL/해시를 기록. " +
                          "flows §1.1: 약정 체결 전제조건(gagr_status_cd=SIGNED).")
    @PostMapping("/{gagrId}/sign")
    public ApiResponse<GuarantorAgreementResponse> sign(
            @PathVariable Long applId,
            @PathVariable Long gagrId,
            @Valid @RequestBody SignGuarantorAgreementRequest req,
            HttpServletRequest http,
            @RequestHeader(value = "User-Agent", required = false) String device) {
        return ApiResponse.ok(service.sign(applId, gagrId, req, http.getRemoteAddr(), device));
    }

    @Operation(summary = "보증 약정 취소",
            description = "REGISTERED 또는 SIGNED → CANCELED. 사후 해지의 약정 영향은 본 단계 외.")
    @PostMapping("/{gagrId}/cancel")
    public ApiResponse<GuarantorAgreementResponse> cancel(
            @PathVariable Long applId,
            @PathVariable Long gagrId,
            @RequestBody(required = false) @Valid CancelGuarantorAgreementRequest req) {
        return ApiResponse.ok(service.cancel(applId, gagrId, req));
    }

    @Operation(summary = "보증 약정 목록 조회")
    @GetMapping
    public ApiResponse<GuarantorAgreementListResponse> list(@PathVariable Long applId) {
        return ApiResponse.ok(service.list(applId));
    }
}
