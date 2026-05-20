package com.bank.loan.autodebit.dto;

public record AutoDebitRunResponse(
        String baseDate,
        int totalCandidates,
        int processed,
        int skipped
) {
    public static AutoDebitRunResponse of(String baseDate, int total, int processed, int skipped) {
        return new AutoDebitRunResponse(baseDate, total, processed, skipped);
    }
}
