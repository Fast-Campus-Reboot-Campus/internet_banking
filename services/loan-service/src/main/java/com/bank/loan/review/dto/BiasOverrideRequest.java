package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BiasOverrideRequest(
        @NotNull Long overrideBy,
        @NotBlank String overrideReason
) {}
