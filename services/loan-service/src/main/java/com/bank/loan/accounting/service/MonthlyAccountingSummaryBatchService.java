package com.bank.loan.accounting.service;

import com.bank.loan.accounting.domain.MonthlyAccountingSummary;
import com.bank.loan.accounting.dto.MonthlySummaryRunResponse;
import com.bank.loan.accounting.repository.AccountingSummaryQuery;
import com.bank.loan.accounting.repository.MonthlyAccountingSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 월별 회계 요약 산출 (EOM 단일 스텝).
 *
 * baseMonth (YYYYMM) → startDate/endDate (YYYYMMDD) 변환 후 집계.
 *
 * 매출 합계: 그 달의 일별 트랜잭션 합 (interest_accrual, overdue_accrual, repayment_transaction, loan_execution).
 * 월말 통계: base_month_end_date 시점의 ACTIVE 약정/연체/NPL 카운트.
 *   (현재 시각 기준이라 늦게 실행할수록 부정확해질 수 있음 — EOM 은 매월 1일 새벽에 실행되는 전제)
 *
 * 멱등: UNIQUE(summary_month) — 이미 적재된 월은 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyAccountingSummaryBatchService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE  = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MonthlyAccountingSummaryRepository repository;
    private final AccountingSummaryQuery query;

    @Transactional
    public MonthlySummaryRunResponse run(String baseMonth) {
        if (repository.existsBySummaryMonth(baseMonth)) {
            log.info("[monthly-summary] baseMonth={} 이미 적재됨 — skip", baseMonth);
            return MonthlySummaryRunResponse.skipped(baseMonth);
        }

        LocalDate firstDay = LocalDate.parse(baseMonth + "01", DATE);
        LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        String startDate = firstDay.format(DATE);
        String endDate   = lastDay.format(DATE);

        long interestRevenue        = query.sumMonthlyInterest(startDate, endDate);
        long overdueInterestRevenue = query.sumMonthlyOverdueInterest(startDate, endDate);
        var autoDebit               = query.sumMonthlyAutoDebit(startDate, endDate);
        var disbursement            = query.sumMonthlyDisbursement(startDate, endDate);
        int monthEndActiveCntr      = query.countActiveContracts();
        int monthEndActiveDlq       = query.countActiveDelinquencies();
        var npl                     = query.sumNpl();

        MonthlyAccountingSummary saved = repository.save(MonthlyAccountingSummary.builder()
                .summaryMonth(baseMonth)
                .baseMonthStartDate(startDate)
                .baseMonthEndDate(endDate)
                .interestRevenue(interestRevenue)
                .overdueInterestRevenue(overdueInterestRevenue)
                .autoDebitPrincipal(autoDebit.principal())
                .autoDebitInterest(autoDebit.interest())
                .autoDebitOverdueInterest(autoDebit.overdueInterest())
                .autoDebitCount(autoDebit.count())
                .newDisbursedAmount(disbursement.amount())
                .newDisbursedCount(disbursement.count())
                .monthEndActiveContracts(monthEndActiveCntr)
                .monthEndActiveDelinquencies(monthEndActiveDlq)
                .monthEndNplCount(npl.count())
                .monthEndNplPrincipal(npl.principal())
                .summarizedAt(OffsetDateTime.now())
                .build());

        log.info("[monthly-summary] baseMonth={} range={}~{} interest={} overdueInt={} autoDebitCnt={} disbursedCnt={} npl={}",
                baseMonth, startDate, endDate, interestRevenue, overdueInterestRevenue,
                autoDebit.count(), disbursement.count(), npl.count());

        return new MonthlySummaryRunResponse(
                true, baseMonth,
                interestRevenue, overdueInterestRevenue,
                autoDebit.principal() + autoDebit.interest() + autoDebit.overdueInterest(),
                autoDebit.count(),
                disbursement.amount(), disbursement.count(),
                monthEndActiveCntr, monthEndActiveDlq,
                npl.count(), npl.principal());
    }
}
