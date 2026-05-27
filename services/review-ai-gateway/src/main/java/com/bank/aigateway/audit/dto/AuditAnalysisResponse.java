package com.bank.aigateway.audit.dto;

public record AuditAnalysisResponse(
        String analysisType,
        String conclusion,
        String reasoningSummary,
        double confidenceScore,
        int inputTokens,
        int outputTokens
) {}
