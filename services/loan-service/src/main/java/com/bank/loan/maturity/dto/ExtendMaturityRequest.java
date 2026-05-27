package com.bank.loan.maturity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 만기 연장 요청. extendedPeriodMo 만큼 current_maturity_date 가 증가한다.
 * 최대 60개월(5년) 한도 — 정책상 한 번에 5년 이상 연장은 별도 결재로 후속.
 */
public record ExtendMaturityRequest(

        @NotNull @Min(1) @Max(60) Integer extendedPeriodMo,

        @Size(max = 50) String extensionTypeCd,

        @Size(max = 500) String extensionReason
) {
}
