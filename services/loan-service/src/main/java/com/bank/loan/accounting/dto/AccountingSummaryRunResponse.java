package com.bank.loan.accounting.dto;

/**
 * 일일 회계 요약 산출 결과.
 *
 *   created           true: 신규 적재 / false: 이미 존재해 skip
 *   summaryDate       기준일 YYYYMMDD
 *   interestRevenue 외  적재된 합계 (참고용)
 */
public record AccountingSummaryRunResponse(
        boolean created,
        String summaryDate,
        long interestRevenue,
        long overdueInterestRevenue,
        long autoDebitTotal,
        int  autoDebitCount,
        long disbursedAmount,
        int  disbursedCount,
        int  activeContractCount,
        int  activeDelinquencyCount
) {
    public static AccountingSummaryRunResponse skipped(String summaryDate) {
        return new AccountingSummaryRunResponse(false, summaryDate, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
