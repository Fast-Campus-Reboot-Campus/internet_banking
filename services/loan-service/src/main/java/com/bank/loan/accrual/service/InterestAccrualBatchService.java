package com.bank.loan.accrual.service;

import com.bank.loan.accrual.domain.InterestAccrual;
import com.bank.loan.accrual.dto.InterestAccrualRunResponse;
import com.bank.loan.accrual.repository.InterestAccrualRepository;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 일별 이자 발생 배치 (flows §1.1 부수효과, §2.2).
 *
 * 보통 매일 새벽 — 자동이체 / 연체 rollover 와는 독립적으로 영업일·휴일 모두 발생.
 *
 * 계약별 처리:
 *   principalBalance      = contractedAmount - Σ(PAID 회차 scheduled_principal)
 *   dailyInterestAmt      = principalBalance × rateBps / 10000 / 365  (ACT/365, HALF_EVEN)
 *   cumulativeInterestAmt = (이전 가장 최근 누적) + dailyInterestAmt
 *
 * skip 케이스:
 *   - 계약이 ACTIVE 아님 (drawdown 전 SIGNED, 종결 CLOSED 등)
 *   - principalBalance <= 0 (전액 상환)
 *   - 같은 (cntr_id, accrual_date) 이미 INSERT 됨 (UNIQUE 멱등)
 *
 * 영업일 보정·상품별 day_count_basis 차등은 본 단계 범위 밖.
 */
@Service
@RequiredArgsConstructor
public class InterestAccrualBatchService {

    private static final Logger log = LoggerFactory.getLogger(InterestAccrualBatchService.class);

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal BPS_TO_DECIMAL = BigDecimal.valueOf(10_000);
    private static final BigDecimal DAYS_PER_YEAR  = BigDecimal.valueOf(365);

    private final InterestAccrualRepository accrualRepository;
    private final LoanContractRepository contractRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    @Transactional
    public InterestAccrualRunResponse run(String baseDate) {
        List<LoanContract> active = contractRepository
                .findByCntrStatusCdAndDeletedAtIsNullOrderByCntrIdAsc(LoanContract.STATUS_ACTIVE);

        OffsetDateTime now = OffsetDateTime.now();
        int processed = 0;
        int skipped = 0;

        for (LoanContract contract : active) {
            try {
                if (accrualRepository.existsByCntrIdAndAccrualDate(contract.getCntrId(), baseDate)) {
                    skipped++;
                    continue;
                }
                long paid = scheduleRepository.sumPaidPrincipal(contract.getCntrId());
                long principalBalance = contract.getContractedAmount() - paid;
                if (principalBalance <= 0) {
                    skipped++;
                    continue;
                }

                long daily = computeDailyInterest(principalBalance, contract.getTotalRateBps());
                long previousCumul = accrualRepository
                        .findFirstByCntrIdAndAccrualDateLessThanOrderByAccrualDateDesc(contract.getCntrId(), baseDate)
                        .map(InterestAccrual::getCumulativeInterestAmt)
                        .orElse(0L);

                accrualRepository.save(InterestAccrual.builder()
                        .cntrId(contract.getCntrId())
                        .accrualDate(baseDate)
                        .principalBalance(principalBalance)
                        .appliedRateBps(contract.getTotalRateBps())
                        .dayCountBasisCd(InterestAccrual.BASIS_ACT_365)
                        .dailyInterestAmt(daily)
                        .cumulativeInterestAmt(previousCumul + daily)
                        .iaccStatusCd(InterestAccrual.STATUS_ACCRUED)
                        .accruedAt(now)
                        .build());
                processed++;
            } catch (RuntimeException e) {
                log.warn("interest-accrual failed for cntrId={} baseDate={}: {}",
                        contract.getCntrId(), baseDate, e.toString());
                skipped++;
            }
        }

        return InterestAccrualRunResponse.of(baseDate, active.size(), processed, skipped);
    }

    private long computeDailyInterest(long principal, int rateBps) {
        return BigDecimal.valueOf(principal)
                .multiply(BigDecimal.valueOf(rateBps), MC)
                .divide(BPS_TO_DECIMAL.multiply(DAYS_PER_YEAR), MC)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
    }
}
