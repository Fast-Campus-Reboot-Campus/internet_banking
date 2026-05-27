package com.bank.loan.calendar.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.calendar.dto.BusinessCalendarListResponse;
import com.bank.loan.calendar.dto.BusinessCalendarResponse;
import com.bank.loan.calendar.dto.BusinessDayCheckResponse;
import com.bank.loan.calendar.dto.RegisterBusinessCalendarRequest;
import com.bank.loan.calendar.dto.UpdateBusinessCalendarRequest;
import com.bank.loan.calendar.service.BusinessDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "영업일 캘린더", description = "BusinessCalendar - 휴일 등록·조회 (자동이체 일배치가 의존)")
@RestController
@RequestMapping("/api/business-calendar")
@RequiredArgsConstructor
@Validated
public class BusinessCalendarController {

    private final BusinessDayService service;

    @Operation(summary = "캘린더 항목 등록", description = "공휴일/임시휴일을 명시적으로 등록. 미등록 일자는 요일 기반 fallback.")
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessCalendarResponse>> register(
            @Valid @RequestBody RegisterBusinessCalendarRequest req) {
        BusinessCalendarResponse saved = service.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "캘린더 항목 수정")
    @PutMapping("/{calId}")
    public ApiResponse<BusinessCalendarResponse> update(
            @PathVariable Long calId,
            @Valid @RequestBody UpdateBusinessCalendarRequest req) {
        return ApiResponse.ok(service.update(calId, req));
    }

    @Operation(summary = "일자별 캘린더 항목 단건 조회", description = "미등록 일자는 404.")
    @GetMapping("/by-date")
    public ApiResponse<BusinessCalendarResponse> getByDate(
            @RequestParam("calDate") @Pattern(regexp = "\\d{8}") String calDate) {
        return ApiResponse.ok(service.getByDate(calDate));
    }

    @Operation(summary = "기간별 캘린더 목록 조회")
    @GetMapping
    public ApiResponse<BusinessCalendarListResponse> listRange(
            @RequestParam("from") @Pattern(regexp = "\\d{8}") String fromDate,
            @RequestParam("to")   @Pattern(regexp = "\\d{8}") String toDate) {
        return ApiResponse.ok(service.listRange(fromDate, toDate));
    }

    @Operation(summary = "영업일 여부 판정",
            description = "DB row 있으면 그 값, 없으면 요일 fallback (토/일=비영업일). " +
                          "source=CALENDAR 또는 WEEKDAY_FALLBACK 로 출처 명시.")
    @GetMapping("/check")
    public ApiResponse<BusinessDayCheckResponse> check(
            @RequestParam("calDate") @Pattern(regexp = "\\d{8}") String calDate) {
        return ApiResponse.ok(service.check(calDate));
    }

    @Operation(summary = "캘린더 항목 soft delete")
    @DeleteMapping("/{calId}")
    public ResponseEntity<Void> delete(@PathVariable Long calId) {
        service.delete(calId);
        return ResponseEntity.noContent().build();
    }
}
