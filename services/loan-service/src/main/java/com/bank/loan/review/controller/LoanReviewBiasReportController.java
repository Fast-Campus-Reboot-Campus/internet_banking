package com.bank.loan.review.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.review.dto.AiReviewAdviceResponse;
import com.bank.loan.review.dto.BiasOverrideRequest;
import com.bank.loan.review.dto.BiasReportRequest;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.service.LoanReviewAcknowledgeBiasService;
import com.bank.loan.review.service.LoanReviewBiasReportService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "편향 검증", description = "LoanReviewBiasReport - 편향 에이전트 리포트 수신 및 조회")
@RestController
@RequiredArgsConstructor
public class LoanReviewBiasReportController {

    private final LoanReviewBiasReportService biasReportService;
    private final LoanReviewAcknowledgeBiasService acknowledgeBiasService;

    @Operation(summary = "편향 리포트 수신 (internal)",
            description = "편향 에이전트가 분석 결과를 밀어넣는 내부 전용 API. "
                    + "BIAS_REVIEWING 상태가 아니더라도 advice 는 저장되나 severity 캐시는 갱신하지 않음 (재전송 무시). "
                    + "외부 노출 금지.")
    @PostMapping("/api/internal/loan-reviews/{revId}/bias-report")
    public ResponseEntity<ApiResponse<AiReviewAdviceResponse>> appendBiasReport(
            @PathVariable Long revId,
            @Valid @RequestBody BiasReportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(biasReportService.append(revId, req)));
    }

    @Operation(summary = "AI 심사 advice 목록 조회",
            description = "해당 본심사에 적재된 모든 AI advice 를 최신순으로 반환. 편향 리포트, 요약, 거절서 등 포함.")
    @GetMapping("/api/loan-reviews/{revId}/advices")
    public ApiResponse<List<AiReviewAdviceResponse>> listAdvices(@PathVariable Long revId) {
        return ApiResponse.ok(biasReportService.listByRev(revId));
    }

    @Operation(summary = "편향 BLOCKED 우회 승인 (상급자)",
            description = "severity=BLOCKED 인 편향 결과를 상급자가 우회 승인. "
                    + "BIAS_REVIEWING 상태일 때만 허용. 우회 후 심사원이 acknowledge-bias 호출 가능.")
    @PostMapping("/api/loan-reviews/{revId}/bias-override")
    public ApiResponse<LoanReviewResponse> biasOverride(
            @PathVariable Long revId,
            @Valid @RequestBody BiasOverrideRequest req) {
        return ApiResponse.ok(acknowledgeBiasService.biasOverride(revId, req));
    }
}
