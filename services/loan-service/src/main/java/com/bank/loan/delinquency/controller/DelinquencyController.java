package com.bank.loan.delinquency.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.delinquency.dto.DelinquencyResponse;
import com.bank.loan.delinquency.dto.DelinquencySnapshotListResponse;
import com.bank.loan.delinquency.service.DelinquencyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "연체", description = "Delinquency - 계약별 활성 연체 + 일별 스냅샷 조회")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/delinquency")
@RequiredArgsConstructor
public class DelinquencyController {

    private final DelinquencyQueryService service;

    @Operation(summary = "활성 연체 조회",
            description = "계약의 ACTIVE dlq 단건. 활성 연체가 없으면 LOAN_100.")
    @GetMapping
    public ApiResponse<DelinquencyResponse> getActive(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.getActive(cntrId));
    }

    @Operation(summary = "연체 일별 스냅샷 목록",
            description = "활성 dlq 의 snapshot 을 snapshot_date 오름차순으로 반환.")
    @GetMapping("/snapshots")
    public ApiResponse<DelinquencySnapshotListResponse> listSnapshots(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.listSnapshots(cntrId));
    }
}
