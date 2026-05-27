package com.bank.loan.guarantor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignGuarantorAgreementRequest(
        @NotBlank @Size(max = 500) String signedDocUrl,
        @NotBlank @Size(max = 128) String signedDocHash
) {
}
