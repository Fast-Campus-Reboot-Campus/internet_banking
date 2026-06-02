package com.bank.loan.audit.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BreakGlassRequest {

    @NotNull
    private Long applId;

    @NotBlank
    private String reason;
}
