package com.bank.customer.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCreditRatingRequest(
        @NotBlank String ratingCode,
        @NotBlank String evaluationDate,
        @NotBlank String agencyCode
) {}
