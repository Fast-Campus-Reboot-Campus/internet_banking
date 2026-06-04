package com.bank.customer.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeGradeRequest(
        @NotBlank String newGradeCode,
        @NotBlank String reasonCode,
        String reasonDetail,
        boolean systemTriggered
) {}
