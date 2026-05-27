package com.bank.loan.accounting.dto;

/**
 * 월별 회계 요약 산출 결과.
 *
 *   created           true: 신규 적재 / false: 이미 존재 skip
 *   summaryMonth      YYYYMM
 *   interestRevenue 외  적재된 합계 (참고용)
 */
public record MonthlySummaryRunResponse(
        boolean created,
        String summaryMonth,
        long interestRevenue,
        long overdueInterestRevenue,
        long autoDebitTotal,
        int  autoDebitCount,
        long newDisbursedAmount,
        int  newDisbursedCount,
        int  monthEndActiveContracts,
        int  monthEndActiveDelinquencies,
        int  monthEndNplCount,
        long monthEndNplPrincipal
) {
    public static MonthlySummaryRunResponse skipped(String summaryMonth) {
        return new MonthlySummaryRunResponse(false, summaryMonth, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
