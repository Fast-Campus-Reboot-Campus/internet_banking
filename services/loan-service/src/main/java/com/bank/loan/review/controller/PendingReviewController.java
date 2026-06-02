package com.bank.loan.review.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.ReviewStatsResponse;
import com.bank.loan.review.service.LoanReviewApproverService;
import com.bank.loan.review.service.LoanReviewAutoDecideService;
import com.bank.loan.review.service.LoanReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "본심사 조회",
        description = "LoanReview - 권고 목록 / 통계 등 본심사 조회용 endpoint")
@RestController
@RequestMapping("/api/loan-reviews")
@RequiredArgsConstructor
@Validated
public class PendingReviewController {

    private final LoanReviewService service;
    private final LoanReviewAutoDecideService autoDecideService;
    private final LoanReviewApproverService approverService;

    @Operation(summary = "확정 대기 권고 목록",
            description = "자동 결정으로 PENDING_APPROVAL 상태가 된 본심사를 reviewedAt 오름차순(오래된 권고 우선)으로 반환. "
                    + "심사관은 본 목록에서 권고를 골라 POST /api/loan-applications/{applId}/review/confirm 으로 확정.")
    @GetMapping("/pending")
    public ApiResponse<List<LoanReviewResponse>> listPending() {
        return ApiResponse.ok(autoDecideService.listPending());
    }

    @Operation(summary = "승인자 대기 목록",
            description = "PENDING_APPROVER 상태의 본심사를 reviewedAt 오름차순으로 반환. "
                    + "승인자는 본 목록에서 건을 골라 POST /api/loan-applications/{applId}/review/approver-approve 로 확정.")
    @GetMapping("/pending-approver")
    public ApiResponse<List<LoanReviewResponse>> listPendingApprover() {
        return ApiResponse.ok(approverService.listPendingApprover());
    }

    @Operation(summary = "본사 상신 건 목록",
            description = "이상거래로 상신된 본심사 목록. ROLE_HQ_REVIEWER 전용. "
                    + "escalatedAt 기준 최신순 정렬. page/size 파라미터로 페이징.")
    @GetMapping("/escalated")
    public ApiResponse<Page<LoanReviewResponse>> listEscalated(
            @PageableDefault(size = 20, sort = "escalatedAt") Pageable pageable) {
        return ApiResponse.ok(service.listEscalated(pageable));
    }

    @Operation(summary = "본심사 통계",
            description = "기간(reviewedAt) 내 본심사 row 를 revTypeCd × revDecisionCd, revStatusCd, "
                    + "rejectReasonCd 별로 집계. 운영 대시보드용. 날짜는 yyyyMMdd, to 는 inclusive.")
    @GetMapping("/stats")
    public ApiResponse<ReviewStatsResponse> stats(
            @RequestParam @Pattern(regexp = "\\d{8}") String from,
            @RequestParam @Pattern(regexp = "\\d{8}") String to) {
        return ApiResponse.ok(service.stats(from, to));
    }
}
