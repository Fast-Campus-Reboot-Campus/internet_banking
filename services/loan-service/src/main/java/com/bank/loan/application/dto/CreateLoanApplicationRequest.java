package com.bank.loan.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLoanApplicationRequest(

        @NotNull Long customerId,
        @NotNull Long prodId,

        @NotBlank @Size(max = 50) String channelCd,

        @NotNull @Min(1) Long requestedAmount,
        @NotNull @Min(1) Integer requestedPeriodMo,

        @Size(max = 50) String loanPurposeCd,
        @Size(max = 50) String repaymentMethodCd,

        @Min(0) Long estimatedIncomeAmt,
        @Size(max = 50) String employmentTypeCd
) {
}
