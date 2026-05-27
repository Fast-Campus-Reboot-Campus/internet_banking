package com.bank.loan.maturity.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.maturity.dto.ExtendMaturityRequest;
import com.bank.loan.maturity.dto.MaturityResponse;
import com.bank.loan.maturity.service.MaturityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "만기관리", description = "Maturity - 약정 만기일 조회 + 연장")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/maturity")
@RequiredArgsConstructor
public class MaturityController {

    private final MaturityService service;

    @Operation(summary = "만기 정보 조회",
            description = "약정 체결 시 자동 생성된 만기 정보. ACTIVE/MATURED/CLOSED 와 연장 이력.")
    @GetMapping
    public ApiResponse<MaturityResponse> get(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.get(cntrId));
    }

    @Operation(summary = "만기 연장",
            description = "current_maturity_date 를 N개월 연장하고 extension_count 증가. " +
                          "original_maturity_date 는 불변. ACTIVE/MATURED 에서만 가능.")
    @PostMapping("/extend")
    public ApiResponse<MaturityResponse> extend(
            @PathVariable Long cntrId,
            @Valid @RequestBody ExtendMaturityRequest req) {
        return ApiResponse.ok(service.extend(cntrId, req));
    }
}
