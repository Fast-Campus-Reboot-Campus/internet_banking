package com.bank.loan.review.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.review.dto.AddReviewCheckLogRequest;
import com.bank.loan.review.dto.ReviewCheckLogResponse;
import com.bank.loan.review.service.ReviewCheckLogService;
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

import java.util.List;

@Tag(name = "본심사 체크 로그", description = "ReviewCheckLog - 본심사 항목별 체크 결과 이력")
@RestController
@RequestMapping("/api/loan-reviews/{revId}/checks")
@RequiredArgsConstructor
public class ReviewCheckLogController {

    private final ReviewCheckLogService service;

    @Operation(summary = "본심사 체크 로그 목록 조회",
            description = "자동 적재 5건 + 수동 추가분을 시간순으로 반환.")
    @GetMapping
    public ApiResponse<List<ReviewCheckLogResponse>> list(@PathVariable Long revId) {
        return ApiResponse.ok(service.list(revId));
    }

    @Operation(summary = "본심사 체크 로그 수동 추가",
            description = "심사관이 서류확인 / 신원확인 / 부수거래 / 기타 항목을 직접 기록. "
                    + "자동 적재 항목 코드는 거부 (LOAN_043).")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewCheckLogResponse>> add(
            @PathVariable Long revId,
            @RequestBody @Valid AddReviewCheckLogRequest req) {
        ReviewCheckLogResponse saved = service.add(revId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
