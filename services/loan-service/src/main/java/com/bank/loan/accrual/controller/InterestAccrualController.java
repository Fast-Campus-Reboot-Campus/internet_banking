package com.bank.loan.accrual.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.accrual.dto.InterestAccrualListResponse;
import com.bank.loan.accrual.service.InterestAccrualQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이자발생조회", description = "InterestAccrual - 계약별 일별 누적 조회")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/interest-accruals")
@RequiredArgsConstructor
@Validated
public class InterestAccrualController {

    private final InterestAccrualQueryService service;

    @Operation(summary = "일별 이자 누적 조회",
            description = "accrual_date 오름차순. from/to 범위 미지정 시 전체.")
    @GetMapping
    public ApiResponse<InterestAccrualListResponse> list(
            @PathVariable Long cntrId,
            @RequestParam(value = "from", required = false) @Pattern(regexp = "\\d{8}") String from,
            @RequestParam(value = "to",   required = false) @Pattern(regexp = "\\d{8}") String to) {
        return ApiResponse.ok(service.list(cntrId, from, to));
    }
}
