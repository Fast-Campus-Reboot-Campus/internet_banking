package com.bank.loan.ratechange.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.ratechange.dto.CreateRateChangeRequest;
import com.bank.loan.ratechange.dto.RateChangeApplyResponse;
import com.bank.loan.ratechange.dto.RateChangeHistoryListResponse;
import com.bank.loan.ratechange.service.RateChangeService;
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

@Tag(name = "금리변경", description = "RateChange - 금리 변경 적용 + 이력")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/rate-changes")
@RequiredArgsConstructor
public class RateChangeController {

    private final RateChangeService service;

    @Operation(summary = "금리 변경 적용",
            description = "계약 금리를 갱신하고 appliedStartDate 이후 활성 회차(DUE/OVERDUE)를 " +
                          "SUPERSEDED 처리한 뒤 남은 원금 기준으로 신규 버전 스케줄을 생성한다. " +
                          "모든 회차가 PAID 인 경우 금리 갱신·이력만 남기고 스케줄은 그대로.")
    @PostMapping
    public ResponseEntity<ApiResponse<RateChangeApplyResponse>> apply(
            @PathVariable Long cntrId,
            @Valid @RequestBody CreateRateChangeRequest req) {
        RateChangeApplyResponse saved = service.apply(cntrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "금리 변경 이력 조회",
            description = "계약의 금리 변경 이력을 changed_at 오름차순으로 반환한다.")
    @GetMapping
    public ApiResponse<RateChangeHistoryListResponse> list(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.listHistory(cntrId));
    }
}
