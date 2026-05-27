package com.bank.loan.consent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCreditConsentRequest(

        @NotBlank @Size(max = 50) String consentTypeCd,
        @NotBlank @Size(max = 50) String consentScopeCd,
        @NotBlank @Size(max = 50) String consentTargetCd,

        @Size(max = 50)  String consentMethodCd,
        @Size(max = 100) String consentToken,
        @Size(max = 500) String signedDocUrl,
        @Size(max = 128) String signedDocHash,

        @Pattern(regexp = "\\d{8}") String retentionUntil
) {
}
