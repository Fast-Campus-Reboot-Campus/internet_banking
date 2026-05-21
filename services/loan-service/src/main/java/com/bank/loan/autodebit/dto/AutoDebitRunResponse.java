package com.bank.loan.autodebit.dto;

public record AutoDebitRunResponse(
        String baseDate,
        int totalCandidates,
        int processed,
        int skipped,
        String skipReason
) {
    public static final String REASON_NON_BUSINESS_DAY = "NON_BUSINESS_DAY";

    public static AutoDebitRunResponse of(String baseDate, int total, int processed, int skipped) {
        return new AutoDebitRunResponse(baseDate, total, processed, skipped, null);
    }

    public static AutoDebitRunResponse skippedNonBusinessDay(String baseDate) {
        return new AutoDebitRunResponse(baseDate, 0, 0, 0, REASON_NON_BUSINESS_DAY);
    }
}
