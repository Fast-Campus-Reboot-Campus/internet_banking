package com.bank.loan.partialrepayment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회차 부분상환 요청.
 *
 *   installmentNo  대상 회차 번호 (최신 버전 기준)
 *   amount         부분 납부 금액 (1 이상, 회차 잔액 이하)
 *   channelCd      채널 코드 (옵션, 기본 MANUAL)
 *   valueDate      거래일 YYYYMMDD (옵션, 회계용)
 *
 * 납부 후 회차 누적이 scheduled_total 과 같아지면 자동으로 PAID 로 전이.
 * 그렇지 않으면 PARTIAL_PAID 로 전이/유지.
 */
public record PartialRepayRequest(
        @NotNull Integer installmentNo,
        @NotNull @Min(1) Long amount,
        @Size(max = 50) String channelCd,
        @Pattern(regexp = "\\d{8}") String valueDate
) {
}
