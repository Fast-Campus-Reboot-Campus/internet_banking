package com.bank.loan.contract.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 약정한도 설정 요청.
 *
 * cntrStartDate / cntrEndDate 미지정 시 서버가 자동 계산
 *   - start = today
 *   - end   = start + contractedPeriodMo months
 *
 * totalRateBps 미지정 시 서버가 자동 계산
 *   - totalRateBps = baseRateBps + spreadBps - preferentialRateBps
 */
public record CreateContractRequest(

        @NotNull Long applId,

        @NotNull @Min(1) Long contractedAmount,
        @NotNull @Min(1) Integer contractedPeriodMo,

        @NotNull @Min(0) Integer baseRateBps,
        @Min(0)          Integer spreadBps,
        @Min(0)          Integer preferentialRateBps,
        @Min(0)          Integer totalRateBps,

        @NotBlank @Size(max = 50) String rateTypeCd,
        @NotBlank @Size(max = 50) String repaymentMethodCd,

        @Size(max = 10) String currencyCd,

        @Pattern(regexp = "\\d{8}") String cntrStartDate,
        @Pattern(regexp = "\\d{8}") String cntrEndDate,

        @Size(max = 500) String cntrDocUrl,
        @Size(max = 128) String cntrDocHash
) {
}
