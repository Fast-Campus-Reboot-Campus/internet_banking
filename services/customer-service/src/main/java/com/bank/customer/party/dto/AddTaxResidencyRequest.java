package com.bank.customer.party.dto;

import jakarta.validation.constraints.NotBlank;

public record AddTaxResidencyRequest(
        @NotBlank String residentTypeCode,
        String taxCountryCode,
        String foreignTin,
        Integer withholdingRateBps,
        @NotBlank String taxResidencyConfirmDate
) {}
