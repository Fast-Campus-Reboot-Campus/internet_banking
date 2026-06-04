package com.bank.customer.party.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePassportRequest(
        @NotBlank String passportNo,
        @NotBlank String countryCode,
        @NotBlank String expiryDate
) {}
