package com.bank.loan.delinquency.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.delinquency.dto.DelinquencyRolloverResponse;
import com.bank.loan.delinquency.service.DelinquencyRolloverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 연체 일배치 트리거 (internal). 보통 매일 새벽 자동이체 직후 호출된다.
 */
@Tag(name = "연체배치", description = "Delinquency - rollover 일배치 (internal)")
@RestController
@RequestMapping("/api/internal/delinquency")
@RequiredArgsConstructor
@Validated
public class DelinquencyRolloverController {

    private final DelinquencyRolloverService service;

    @Operation(summary = "연체 rollover 실행",
            description = "baseDate 기준으로 due_date 가 지난 DUE 회차를 OVERDUE 로 전이하고 " +
                          "계약별 DELINQUENCY 를 upsert 한다. 활성 연체가 해소되면 RESOLVED 처리.")
    @PostMapping("/rollover")
    public ApiResponse<DelinquencyRolloverResponse> rollover(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {
        return ApiResponse.ok(service.rollover(baseDate));
    }
}
