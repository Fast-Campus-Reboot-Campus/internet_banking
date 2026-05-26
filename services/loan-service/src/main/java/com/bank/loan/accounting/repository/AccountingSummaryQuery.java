package com.bank.loan.accounting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 일일 회계 요약을 위한 합계 쿼리 모음 (JdbcTemplate raw SQL).
 *
 * 여러 도메인(interest_accrual, overdue_accrual, repayment_transaction, loan_execution,
 * loan_contract, delinquency) 의 합계를 한 곳에서 모은다.
 *
 * JPA Repository 마다 sum 메서드를 흩뿌리는 대신 이 클래스 하나에서 raw SQL 로 처리한다 —
 * 회계 요약은 본격 도메인이 아니라 정산용 read-only 집계라 raw SQL 이 의도와 더 잘 맞음.
 */
@Component
@RequiredArgsConstructor
public class AccountingSummaryQuery {

    private final JdbcTemplate jdbc;

    public long sumDailyInterest(String baseDate) {
        return queryLong("""
                SELECT COALESCE(SUM(daily_interest_amt), 0)
                  FROM interest_accrual
                 WHERE accrual_date = ?
                   AND iacc_status_cd = 'ACCRUED'
                """, baseDate);
    }

    public long sumDailyOverdueInterest(String baseDate) {
        return queryLong("""
                SELECT COALESCE(SUM(daily_overdue_interest), 0)
                  FROM overdue_accrual
                 WHERE accrual_date = ?
                   AND oa_status_cd = 'ACCRUED'
                """, baseDate);
    }

    public RepaymentTxnSummary sumAutoDebit(String baseDate) {
        return jdbc.queryForObject("""
                SELECT
                    COALESCE(SUM(principal_amount), 0)         AS principal,
                    COALESCE(SUM(interest_amount), 0)          AS interest,
                    COALESCE(SUM(overdue_interest_amount), 0)  AS overdue_interest,
                    COUNT(*)                                   AS cnt
                FROM repayment_transaction
                WHERE value_date = ?
                  AND channel_cd = 'AUTO_DEBIT'
                  AND rtx_status_cd = 'SUCCESS'
                  AND reversal_yn = 'N'
                  AND deleted_at IS NULL
                """, (rs, n) -> new RepaymentTxnSummary(
                        rs.getLong("principal"),
                        rs.getLong("interest"),
                        rs.getLong("overdue_interest"),
                        rs.getInt("cnt")),
                baseDate);
    }

    public DisbursementSummary sumDisbursement(String baseDate) {
        return jdbc.queryForObject("""
                SELECT
                    COALESCE(SUM(executed_amount), 0) AS amount,
                    COUNT(*)                          AS cnt
                FROM loan_execution
                WHERE value_date = ?
                  AND exec_status_cd = 'DONE'
                  AND deleted_at IS NULL
                """, (rs, n) -> new DisbursementSummary(
                        rs.getLong("amount"),
                        rs.getInt("cnt")),
                baseDate);
    }

    public int countActiveContracts() {
        return queryInt("""
                SELECT COUNT(*)
                  FROM loan_contract
                 WHERE cntr_status_cd = 'ACTIVE'
                   AND deleted_at IS NULL
                """);
    }

    public int countActiveDelinquencies() {
        return queryInt("""
                SELECT COUNT(*)
                  FROM delinquency
                 WHERE dlq_status_cd = 'ACTIVE'
                   AND deleted_at IS NULL
                """);
    }

    // ───────────────────────────────────────────────────────────
    // 월별 (EOM) 합계 — start/end 는 YYYYMMDD inclusive
    // ───────────────────────────────────────────────────────────

    public long sumMonthlyInterest(String startDate, String endDate) {
        return queryLong("""
                SELECT COALESCE(SUM(daily_interest_amt), 0)
                  FROM interest_accrual
                 WHERE accrual_date BETWEEN ? AND ?
                   AND iacc_status_cd = 'ACCRUED'
                """, startDate, endDate);
    }

    public long sumMonthlyOverdueInterest(String startDate, String endDate) {
        return queryLong("""
                SELECT COALESCE(SUM(daily_overdue_interest), 0)
                  FROM overdue_accrual
                 WHERE accrual_date BETWEEN ? AND ?
                   AND oa_status_cd = 'ACCRUED'
                """, startDate, endDate);
    }

    public RepaymentTxnSummary sumMonthlyAutoDebit(String startDate, String endDate) {
        return jdbc.queryForObject("""
                SELECT
                    COALESCE(SUM(principal_amount), 0)         AS principal,
                    COALESCE(SUM(interest_amount), 0)          AS interest,
                    COALESCE(SUM(overdue_interest_amount), 0)  AS overdue_interest,
                    COUNT(*)                                   AS cnt
                FROM repayment_transaction
                WHERE value_date BETWEEN ? AND ?
                  AND channel_cd = 'AUTO_DEBIT'
                  AND rtx_status_cd = 'SUCCESS'
                  AND reversal_yn = 'N'
                  AND deleted_at IS NULL
                """, (rs, n) -> new RepaymentTxnSummary(
                        rs.getLong("principal"),
                        rs.getLong("interest"),
                        rs.getLong("overdue_interest"),
                        rs.getInt("cnt")),
                startDate, endDate);
    }

    public DisbursementSummary sumMonthlyDisbursement(String startDate, String endDate) {
        return jdbc.queryForObject("""
                SELECT
                    COALESCE(SUM(executed_amount), 0) AS amount,
                    COUNT(*)                          AS cnt
                FROM loan_execution
                WHERE value_date BETWEEN ? AND ?
                  AND exec_status_cd = 'DONE'
                  AND deleted_at IS NULL
                """, (rs, n) -> new DisbursementSummary(
                        rs.getLong("amount"),
                        rs.getInt("cnt")),
                startDate, endDate);
    }

    public NplSummary sumNpl() {
        return jdbc.queryForObject("""
                SELECT
                    COUNT(*)                              AS cnt,
                    COALESCE(SUM(dlq_principal_amt), 0)   AS principal
                FROM delinquency
                WHERE dlq_status_cd = 'ACTIVE'
                  AND dlq_stage_cd  = 'STAGE_3'
                  AND deleted_at IS NULL
                """, (rs, n) -> new NplSummary(
                        rs.getInt("cnt"),
                        rs.getLong("principal")));
    }

    public record NplSummary(int count, long principal) {}

    private long queryLong(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    private int queryInt(String sql, Object... args) {
        Integer v = jdbc.queryForObject(sql, Integer.class, args);
        return v == null ? 0 : v;
    }

    public record RepaymentTxnSummary(long principal, long interest, long overdueInterest, int count) {}
    public record DisbursementSummary(long amount, int count) {}
}
