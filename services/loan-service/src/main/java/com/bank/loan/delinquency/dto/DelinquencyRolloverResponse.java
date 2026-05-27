package com.bank.loan.delinquency.dto;

public record DelinquencyRolloverResponse(
        String baseDate,
        int newlyOverdueInstallments,
        int activeDelinquencies,
        int resolvedDelinquencies,
        int snapshotsCreated
) {
    public static DelinquencyRolloverResponse of(String baseDate, int newlyOverdue, int active, int resolved, int snapshots) {
        return new DelinquencyRolloverResponse(baseDate, newlyOverdue, active, resolved, snapshots);
    }
}
