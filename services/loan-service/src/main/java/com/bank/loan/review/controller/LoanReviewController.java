package com.bank.loan.review.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.ReviseReviewRequest;
import com.bank.loan.review.dto.RunReviewRequest;
import com.bank.loan.review.service.LoanReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "본심사", description = "LoanReview - 본심사 결정(APPROVED/REJECTED) 적재 + 신청 상태 전이")
@RestController
@RequestMapping("/api/loan-applications/{applId}/review")
@RequiredArgsConstructor
public class LoanReviewController {

    private final LoanReviewService service;

    @Operation(summary = "본심사 실행",
            description = "사전조건: 신청 PRESCREENED + CB(APPROVE/REVIEW) + DSR PASS. " +
                          "APPROVED 시 한도/금리/기간 자동 산정 (입력값 우선). " +
                          "신청 상태: PRESCREENED → APPROVED/REJECTED. 신청당 1건 (appl_id UNIQUE).")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanReviewResponse>> run(
            @PathVariable Long applId,
            @Valid @RequestBody RunReviewRequest req) {
        LoanReviewResponse saved = service.run(applId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "본심사 결과 조회")
    @GetMapping
    public ApiResponse<LoanReviewResponse> get(@PathVariable Long applId) {
        return ApiResponse.ok(service.get(applId));
    }

    @Operation(summary = "본심사 결정 정정(재심사)",
            description = "사전조건: 신청 APPROVED/REJECTED. CONTRACTED 등 약정 진입 후엔 LOAN_044. "
                    + "결정·한도·금리·기간을 갱신하고 신청 상태를 동기화한다. "
                    + "체크로그에 FINAL_DECISION 정정 기록이 누적되고 status_history 양쪽에 이력이 남는다.")
    @PatchMapping
    public ApiResponse<LoanReviewResponse> revise(
            @PathVariable Long applId,
            @Valid @RequestBody ReviseReviewRequest req) {
        return ApiResponse.ok(service.revise(applId, req));
    }
}
