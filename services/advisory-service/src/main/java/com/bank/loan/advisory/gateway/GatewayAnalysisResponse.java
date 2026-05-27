package com.bank.loan.advisory.gateway;

public record GatewayAnalysisResponse(
        String analysisType,
        String conclusion,
        String reasoningSummary,
        double confidenceScore,
        int inputTokens,
        int outputTokens
) {}
