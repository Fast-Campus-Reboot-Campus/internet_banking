package com.bank.loan.review.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 본심사 실행 요청.
 *
 * revTypeCd : AUTO | MANUAL
 * revDecisionCd : APPROVED | REJECTED
 *
 * APPROVED 시 한도/금리/기간:
 *   approvedAmount / approvedRateBps / approvedPeriodMo 미지정 시 서버 자동 산정
 *     approvedAmount = min(requestedAmount, CB.evalLimit, product.maxAmount)
 *     approvedRateBps = CB.evalRateBps ?? product.baseRateBps
 *     approvedPeriodMo = requestedPeriodMo
 *
 * REJECTED 시 rejectReasonCd 권장.
 */
public record RunReviewRequest(

        @NotBlank @Pattern(regexp = "AUTO|MANUAL") String revTypeCd,

        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String revDecisionCd,

        @Min(0) Long approvedAmount,
        @Min(0) Integer approvedRateBps,
        @Min(1) Integer approvedPeriodMo,

        @Size(max = 50) String rejectReasonCd,
        @Size(max = 500) String revRemark
) {
}
