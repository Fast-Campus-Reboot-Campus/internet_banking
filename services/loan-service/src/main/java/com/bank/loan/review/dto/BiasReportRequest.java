package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BiasReportRequest(
        @NotBlank String severityCd,
        @NotBlank String summary,
        List<Finding> findings,
        String model,
        String modelVersion,
        String promptHash,
        Integer inputToken,
        Integer outputToken,
        Integer latencyMs
) {
    public record Finding(
            @NotNull String code,
            @NotNull String result,
            String detail
    ) {}
}
