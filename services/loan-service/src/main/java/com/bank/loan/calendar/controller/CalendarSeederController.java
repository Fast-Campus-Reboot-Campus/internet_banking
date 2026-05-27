package com.bank.loan.calendar.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.calendar.dto.CalendarSeederRunResponse;
import com.bank.loan.calendar.service.CalendarSeederBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 영업일 캘린더 시드 트리거 (internal).
 * 평소엔 CalendarSeederScheduler 가 매년 12월 1일 자동 실행. 본 엔드포인트는 수동/소급용.
 */
@Tag(name = "캘린더 시드", description = "BusinessCalendar - 연도별 자동 시드 (internal)")
@RestController
@RequestMapping("/api/internal/calendar-seeder")
@RequiredArgsConstructor
@Validated
public class CalendarSeederController {

    private final CalendarSeederBatchService service;

    @Operation(summary = "특정 연도 캘린더 자동 시드",
            description = "주말 + 양력 고정 공휴일은 자동, 음력/대체/임시 공휴일은 운영자가 별도 등록. " +
                          "UNIQUE(cal_date) 로 멱등 — 이미 있는 날짜는 skip.")
    @PostMapping("/run")
    public ApiResponse<CalendarSeederRunResponse> run(
            @RequestParam("year") @Min(2020) @Max(2099) int year) {
        return ApiResponse.ok(service.run(year));
    }
}
