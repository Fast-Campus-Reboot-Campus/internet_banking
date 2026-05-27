package com.bank.loan.review.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 본심사 결정 정정(재심사) 요청.
 *
 * 신청이 APPROVED 또는 REJECTED 일 때만 정정 가능 — CONTRACTED 등 후속 단계 진입 후엔 LOAN_044.
 * 같은 LoanReview row 를 갱신하며, 변경 이력은 status_history + 체크로그 재적재로 남긴다.
 *
 * revDecisionCd : APPROVED | REJECTED
 *
 * APPROVED 로 정정 시 한도/금리/기간:
 *   미지정이면 RunReviewRequest 와 동일 규칙으로 서버 자동 산정
 *     approvedAmount  = min(requestedAmount, CB.evalLimit, product.maxAmount)
 *     approvedRateBps = CB.evalRateBps ?? product.baseRateBps
 *     approvedPeriodMo = requestedPeriodMo
 *
 * REJECTED 로 정정 시 rejectReasonCd 권장.
 * revisitReasonCd 는 정정 사유(예: APPEAL / ERROR_CORRECTION / NEW_EVIDENCE) — 감사 추적용 필수.
 */
public record ReviseReviewRequest(

        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String revDecisionCd,

        @Min(0) Long approvedAmount,
        @Min(0) Integer approvedRateBps,
        @Min(1) Integer approvedPeriodMo,

        @Size(max = 50) String rejectReasonCd,
        @Size(max = 500) String revRemark,

        @NotBlank @Size(max = 50) String revisitReasonCd,

        Long reviewerId
) {
}
