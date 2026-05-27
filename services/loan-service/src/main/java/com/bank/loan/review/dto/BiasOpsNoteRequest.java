package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BiasOpsNoteRequest(
        @NotNull Long opsStaffId,
        @NotBlank String note
) {}
