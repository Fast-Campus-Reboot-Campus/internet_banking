package com.bank.loan.ratechange.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 금리 변경 요청.
 *
 * newTotalRateBps 미지정 시 서버가 자동 계산:
 *   newTotal = newBase + newSpread - newPreferential  (음수가 되면 0)
 *
 * appliedStartDate 이후 첫 회차부터 새 금리가 적용된다. 그 이전 회차·과거 발생 이자는 불변.
 */
public record CreateRateChangeRequest(

        @NotNull @Min(0) Integer newBaseRateBps,

        @Min(0) Integer newSpreadBps,
        @Min(0) Integer newPreferentialRateBps,
        @Min(0) Integer newTotalRateBps,

        @NotBlank @Pattern(regexp = "\\d{8}") String appliedStartDate,

        @NotBlank @Size(max = 50) String rateChangeReasonCd
) {
}
