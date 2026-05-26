package com.bank.loan.accounting.service;

import com.bank.loan.accounting.domain.DailyAccountingSummary;
import com.bank.loan.accounting.dto.AccountingSummaryRunResponse;
import com.bank.loan.accounting.repository.AccountingSummaryQuery;
import com.bank.loan.accounting.repository.DailyAccountingSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 일일 회계 요약 산출 배치 (EOD 후반 스텝).
 *
 * 전제: 본 잡 호출 시점에 같은 baseDate 의 interest_accrual / overdue_accrual / 자동이체
 *      트랜잭션 / 신규 실행이 모두 적재되어 있어야 한다.
 *
 * 멱등성: UNIQUE(summary_date) — 이미 존재하면 skip (운영자가 의도적 재산출 원할 경우 row 삭제 후 재실행).
 *
 * 본격 복식부기 전표는 본 단계 범위 외 — 정산용 read-only 집계만 제공.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingSummaryBatchService {

    private final DailyAccountingSummaryRepository summaryRepository;
    private final AccountingSummaryQuery query;

    @Transactional
    public AccountingSummaryRunResponse run(String baseDate) {
        if (summaryRepository.existsBySummaryDate(baseDate)) {
            log.info("[accounting-summary] baseDate={} 이미 적재됨 — skip", baseDate);
            return AccountingSummaryRunResponse.skipped(baseDate);
        }

        long interestRevenue        = query.sumDailyInterest(baseDate);
        long overdueInterestRevenue = query.sumDailyOverdueInterest(baseDate);
        var autoDebit               = query.sumAutoDebit(baseDate);
        var disbursement            = query.sumDisbursement(baseDate);
        int activeContracts         = query.countActiveContracts();
        int activeDelinquencies     = query.countActiveDelinquencies();

        DailyAccountingSummary saved = summaryRepository.save(DailyAccountingSummary.builder()
                .summaryDate(baseDate)
                .interestRevenue(interestRevenue)
                .overdueInterestRevenue(overdueInterestRevenue)
                .autoDebitPrincipal(autoDebit.principal())
                .autoDebitInterest(autoDebit.interest())
                .autoDebitOverdueInterest(autoDebit.overdueInterest())
                .autoDebitCount(autoDebit.count())
                .disbursedAmount(disbursement.amount())
                .disbursedCount(disbursement.count())
                .activeContractCount(activeContracts)
                .activeDelinquencyCount(activeDelinquencies)
                .summarizedAt(OffsetDateTime.now())
                .build());

        log.info("[accounting-summary] baseDate={} interest={} overdueInterest={} " +
                        "autoDebit(P/I/OI/cnt)={}/{}/{}/{} disbursed={}({}) activeCntr={} activeDlq={}",
                baseDate, interestRevenue, overdueInterestRevenue,
                autoDebit.principal(), autoDebit.interest(), autoDebit.overdueInterest(), autoDebit.count(),
                disbursement.amount(), disbursement.count(),
                activeContracts, activeDelinquencies);

        return new AccountingSummaryRunResponse(
                true, baseDate,
                interestRevenue, overdueInterestRevenue,
                autoDebit.principal() + autoDebit.interest() + autoDebit.overdueInterest(),
                autoDebit.count(),
                disbursement.amount(), disbursement.count(),
                activeContracts, activeDelinquencies);
    }
}
