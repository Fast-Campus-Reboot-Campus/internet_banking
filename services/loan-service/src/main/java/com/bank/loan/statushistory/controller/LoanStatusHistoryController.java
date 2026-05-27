package com.bank.loan.statushistory.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.statushistory.dto.StatusHistoryListResponse;
import com.bank.loan.statushistory.service.LoanStatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상태 이력", description = "StatusHistory - 대출 도메인 객체의 상태 변경 이력 조회")
@RestController
@RequestMapping("/api/status-history")
@RequiredArgsConstructor
@Validated
public class LoanStatusHistoryController {

    private final LoanStatusHistoryService service;

    @Operation(summary = "상태 변경 이력 조회",
            description = "targetTable(예: LOAN_APPLICATION, LOAN_CONTRACT, REPAYMENT_SCHEDULE) 과 "
                    + "targetId 로 변경 시각 오름차순 이력을 반환. targetDomain 은 LOAN 으로 고정.")
    @GetMapping
    public ApiResponse<StatusHistoryListResponse> list(
            @RequestParam("targetTable") @NotBlank String targetTable,
            @RequestParam("targetId") @NotNull Long targetId) {
        return ApiResponse.ok(service.list(targetTable, targetId));
    }
}
