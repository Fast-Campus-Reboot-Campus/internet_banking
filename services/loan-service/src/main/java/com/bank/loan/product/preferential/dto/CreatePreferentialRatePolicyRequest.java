package com.bank.loan.product.preferential.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePreferentialRatePolicyRequest(

        @NotBlank @Size(max = 200) String policyName,
        @NotBlank @Size(max = 50)  String conditionCd,

        @NotNull @Min(0) Integer preferentialRateBps,
        @Min(0)          Integer maxStackBps,

        @Pattern(regexp = "\\d{8}") String effectiveStartDate,
        @Pattern(regexp = "\\d{8}") String effectiveEndDate,

        @Size(max = 500) String policyRemark
) {
}
