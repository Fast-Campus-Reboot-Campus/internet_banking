package com.bank.loan.execution.dto;

import com.bank.common.security.mask.Masking;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DrawdownRequest(

        @NotNull @Min(1) Long executedAmount,

        @Size(max = 10) String currencyCd,

        @Size(max = 10) String disbursementBankCd,

        @NotBlank String disbursementAccountNo,

        @Pattern(regexp = "\\d{8}") String valueDate,

        @Min(0) Long feeAmount
) {
    public String maskedAccount() {
        return Masking.account(disbursementAccountNo);
    }
}
