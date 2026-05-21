package com.bank.loan.ltv.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * LTV 산정 요청.
 *
 * 모든 입력 선택 — 미지정 시 서버가 자동 채움:
 *   appliedColValue   = 최신 담보 감정평가 applied_value
 *   seniorLienAmount  = collateral.senior_lien_amount
 *   requestedAmount   = application.requested_amount
 *   ltvLimitBps       = 7000 (70%)
 */
public record RunLtvCalculationRequest(

        @Min(0) Long appliedColValue,
        @Min(0) Long seniorLienAmount,
        @Min(0) Long requestedAmount,
        @Min(0) Integer ltvLimitBps,

        @Size(max = 50) String calcEngineVersion
) {
}
