package com.bank.loan.accrual.dto;

public record InterestAccrualRunResponse(
        String baseDate,
        int totalCandidates,
        int processed,
        int skipped
) {
    public static InterestAccrualRunResponse of(String baseDate, int total, int processed, int skipped) {
        return new InterestAccrualRunResponse(baseDate, total, processed, skipped);
    }
}
