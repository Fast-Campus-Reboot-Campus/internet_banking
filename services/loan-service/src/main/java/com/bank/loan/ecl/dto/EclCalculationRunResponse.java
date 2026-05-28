package com.bank.loan.ecl.dto;

/**
 * ECL 산출 1회 실행 요약.
 *
 *   baseMonth         YYYYMM
 *   totalCandidates   ACTIVE 약정 수
 *   processed         신규 산출된 row 수
 *   skipped           이미 적재 / 잔액 0 등으로 skip
 *   totalEcl          그 달 ECL 합계 (충당금 총액 참고)
 */
public record EclCalculationRunResponse(
        String baseMonth,
        int totalCandidates,
        int processed,
        int skipped,
        long totalEcl
) {
    public static EclCalculationRunResponse of(String baseMonth, int total, int processed, int skipped, long totalEcl) {
        return new EclCalculationRunResponse(baseMonth, total, processed, skipped, totalEcl);
    }
}
