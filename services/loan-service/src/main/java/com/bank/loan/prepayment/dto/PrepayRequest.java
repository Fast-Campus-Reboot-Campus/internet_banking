package com.bank.loan.prepayment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 중도상환 요청.
 *
 *   amount      원금 일부 또는 전액 (1 이상). 미발생이자/수수료는 본 단계 0으로 처리.
 *   channel     채널 코드 (예: MOBILE, COUNTER). 기본 MANUAL.
 *   valueDate   거래일 YYYYMMDD (옵션, 회계용).
 */
public record PrepayRequest(
        @NotNull @Min(1) Long amount,
        @Size(max = 50) String channelCd,
        @Pattern(regexp = "\\d{8}") String valueDate
) {
}
