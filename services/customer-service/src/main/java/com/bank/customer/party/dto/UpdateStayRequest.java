package com.bank.customer.party.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStayRequest(
        @NotBlank String stayQualificationCode,
        @NotBlank String stayExpiryDate
) {}
