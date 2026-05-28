package com.bank.loan.delinquency.dto;

public record OverdueInterestAccrualRunResponse(
        String baseDate,
        int totalCandidates,
        int processed,
        int skipped
) {
    public static OverdueInterestAccrualRunResponse of(String baseDate, int total, int processed, int skipped) {
        return new OverdueInterestAccrualRunResponse(baseDate, total, processed, skipped);
    }
}
