package com.bank.loan.schedule.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.schedule.dto.RepaymentScheduleListResponse;
import com.bank.loan.schedule.service.RepaymentScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상환스케줄", description = "RepaymentSchedule - 회차별 상환 계획")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/repayment-schedules")
@RequiredArgsConstructor
public class RepaymentScheduleController {

    private final RepaymentScheduleService service;

    @Operation(summary = "상환 스케줄 조회",
            description = "version 미지정 시 최신 버전(V1, 금리변경/중도상환 시 V2...) 의 회차 목록을 " +
                          "installment_no 오름차순으로 반환한다. SUPERSEDED 회차도 version 을 명시하면 조회 가능. " +
                          "스케줄은 최초 drawdown 시 자동 생성된다.")
    @GetMapping
    public ApiResponse<RepaymentScheduleListResponse> list(
            @PathVariable Long cntrId,
            @RequestParam(value = "version", required = false) String version) {
        return ApiResponse.ok(service.list(cntrId, version));
    }
}
