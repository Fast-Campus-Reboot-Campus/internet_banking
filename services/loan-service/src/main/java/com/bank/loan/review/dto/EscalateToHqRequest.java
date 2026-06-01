package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalateToHqRequest(
        @NotBlank @Size(max = 500) String escalateReason
) {}
