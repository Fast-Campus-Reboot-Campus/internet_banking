package com.bank.loan.delinquency.service;

import com.bank.loan.delinquency.domain.Delinquency;
import com.bank.loan.delinquency.domain.OverdueAccrual;
import com.bank.loan.delinquency.dto.OverdueInterestAccrualRunResponse;
import com.bank.loan.delinquency.repository.DelinquencyRepository;
import com.bank.loan.delinquency.repository.OverdueAccrualRepository;
import com.bank.loan.repayment.service.OverdueInterestCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 연체 이자 일별 발생 배치 (flows §2.2 — delinquencyRollover 직후 실행).
 *
 * 대상: dlq_status_cd = ACTIVE 인 연체 계약 전체.
 *
 * 계산식 (ACT/365, 단리):
 *   daily = dlqPrincipalAmt × overdueRateBps × 1 / 10000 / 365
 *   cumulative = 이전 최근 누적 + daily
 *
 * 멱등: UNIQUE(cntr_id, accrual_date) — 같은 baseDate 재실행 시 skip.
 */
@Service
@RequiredArgsConstructor
public class OverdueInterestAccrualBatchService {

    private static final Logger log = LoggerFactory.getLogger(OverdueInterestAccrualBatchService.class);

    private final DelinquencyRepository delinquencyRepository;
    private final OverdueAccrualRepository overdueAccrualRepository;

    @Transactional
    public OverdueInterestAccrualRunResponse run(String baseDate) {
        List<Delinquency> activeDelinquencies =
                delinquencyRepository.findByDlqStatusCdAndDeletedAtIsNull(Delinquency.STATUS_ACTIVE);

        OffsetDateTime now = OffsetDateTime.now();
        int processed = 0;
        int skipped = 0;

        for (Delinquency dlq : activeDelinquencies) {
            try {
                if (overdueAccrualRepository.existsByCntrIdAndAccrualDate(dlq.getCntrId(), baseDate)) {
                    skipped++;
                    continue;
                }

                long daily = OverdueInterestCalculator.compute(
                        dlq.getDlqPrincipalAmt(),
                        dlq.getOverdueRateBps(),
                        1
                );

                long prevCumul = overdueAccrualRepository
                        .findFirstByCntrIdAndAccrualDateLessThanOrderByAccrualDateDesc(dlq.getCntrId(), baseDate)
                        .map(OverdueAccrual::getCumulativeOverdueInterest)
                        .orElse(0L);

                overdueAccrualRepository.save(OverdueAccrual.builder()
                        .cntrId(dlq.getCntrId())
                        .dlqId(dlq.getDlqId())
                        .accrualDate(baseDate)
                        .overduePrincipal(dlq.getDlqPrincipalAmt())
                        .overdueRateBps(dlq.getOverdueRateBps())
                        .dlqDays(dlq.getDlqDays())
                        .dailyOverdueInterest(daily)
                        .cumulativeOverdueInterest(prevCumul + daily)
                        .oaStatusCd(OverdueAccrual.STATUS_ACCRUED)
                        .accruedAt(now)
                        .build());
                processed++;

            } catch (RuntimeException e) {
                log.warn("[overdue-accrual] cntrId={} baseDate={} 실패: {}",
                        dlq.getCntrId(), baseDate, e.toString());
                skipped++;
            }
        }

        return OverdueInterestAccrualRunResponse.of(baseDate, activeDelinquencies.size(), processed, skipped);
    }
}
