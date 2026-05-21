package com.bank.loan.review.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.review.dto.ReviewCheckLogResponse;
import com.bank.loan.review.service.ReviewCheckLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "본심사 체크 로그", description = "ReviewCheckLog - 본심사 항목별 체크 결과 이력 조회")
@RestController
@RequestMapping("/api/loan-reviews/{revId}/checks")
@RequiredArgsConstructor
public class ReviewCheckLogController {

    private final ReviewCheckLogService service;

    @Operation(summary = "본심사 체크 로그 목록 조회",
            description = "본심사 결정 시점에 자동 적재된 항목별 체크 결과를 시간순으로 반환.")
    @GetMapping
    public ApiResponse<List<ReviewCheckLogResponse>> list(@PathVariable Long revId) {
        return ApiResponse.ok(service.list(revId));
    }
}
