package com.bank.loan.repayment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회차 정확액 상환 요청.
 *
 * 본 단계는 부분상환·중도상환·금액 직접지정 미지원 — installmentNo 만 받고
 * 금액·분배는 서버가 스케줄 row 기준으로 산출한다.
 */
public record RepayInstallmentRequest(

        @NotNull @Min(1) Integer installmentNo,

        @Size(max = 50) String channelCd,

        @Pattern(regexp = "\\d{8}") String valueDate
) {
}
