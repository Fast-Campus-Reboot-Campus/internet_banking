package com.bank.loan.accrual.dto;

import java.util.List;

public record InterestAccrualListResponse(
        Long cntrId,
        int totalCount,
        Long sumDailyInterest,
        Long latestCumulativeInterest,
        List<InterestAccrualResponse> items
) {
    public static InterestAccrualListResponse of(Long cntrId, List<InterestAccrualResponse> items) {
        long sumDaily = items.stream().mapToLong(InterestAccrualResponse::dailyInterestAmt).sum();
        long latestCumul = items.isEmpty() ? 0L : items.get(items.size() - 1).cumulativeInterestAmt();
        return new InterestAccrualListResponse(cntrId, items.size(), sumDaily, latestCumul, items);
    }
}
