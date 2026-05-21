package com.bank.loan.dsr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DSR(한도) 산정 요청.
 *
 * 필수: annualIncomeAmt (연소득)
 * 선택: existingPrincipalTotal · existingAnnualRepayAmt (기존 부채) — 미지정 시 0
 *       newAnnualRepayAmt — 미지정 시 서버가 requestedAmount × baseRateBps / period 기반으로 단순 추정
 *       dsrLimitBps — 미지정 시 기본 4000(40%)
 */
public record RunDsrCalculationRequest(

        @NotNull @Min(0) Long annualIncomeAmt,

        @Min(0) Long existingPrincipalTotal,
        @Min(0) Long existingAnnualRepayAmt,

        @Min(0) Long newAnnualRepayAmt,
        @Min(0) Integer dsrLimitBps,

        @Size(max = 50) String dsrRegTypeCd,
        @Size(max = 50) String calcEngineVersion,

        String dsrDetail
) {
}
