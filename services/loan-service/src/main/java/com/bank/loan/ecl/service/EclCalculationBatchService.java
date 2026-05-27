package com.bank.loan.ecl.service;

import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.delinquency.domain.Delinquency;
import com.bank.loan.delinquency.repository.DelinquencyRepository;
import com.bank.loan.ecl.domain.LoanEclSummary;
import com.bank.loan.ecl.dto.EclCalculationRunResponse;
import com.bank.loan.ecl.engine.EclEngine;
import com.bank.loan.ecl.repository.LoanEclSummaryRepository;
import com.bank.loan.guaranteeinsurance.domain.GuaranteeInsurance;
import com.bank.loan.guaranteeinsurance.repository.GuaranteeInsuranceRepository;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IFRS9 ECL 월별 산출 배치 (EOM 잡 스텝).
 *
 * 대상: ACTIVE 약정 전체. 잔액 0 이면 skip.
 * 입력 수집:
 *   principalBalance = contractedAmount - Σ(PAID 회차 scheduled_principal)
 *   delinquencyStageCd = 활성 Delinquency.dlqStageCd (없으면 null)
 *   hasActiveGuaranteeInsurance = GuaranteeInsurance.ISSUED 존재 여부
 *   hasCollateral = false (본 단계 범위 외)
 *
 * 엔진 호출: EclEngine.calculate(input) — Mock 또는 HTTP 어댑터.
 *
 * 멱등: UNIQUE(cntr_id, summary_month) — 동일 baseMonth 재실행 시 약정별 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EclCalculationBatchService {

    private final LoanContractRepository contractRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final DelinquencyRepository delinquencyRepository;
    private final GuaranteeInsuranceRepository guaranteeInsuranceRepository;
    private final LoanEclSummaryRepository eclRepository;
    private final EclEngine eclEngine;

    @Transactional
    public EclCalculationRunResponse run(String baseMonth) {
        List<LoanContract> active = contractRepository
                .findByCntrStatusCdAndDeletedAtIsNullOrderByCntrIdAsc(LoanContract.STATUS_ACTIVE);

        OffsetDateTime now = OffsetDateTime.now();
        int processed = 0, skipped = 0;
        long totalEcl = 0L;

        for (LoanContract contract : active) {
            try {
                Long cntrId = contract.getCntrId();
                if (eclRepository.existsByCntrIdAndSummaryMonth(cntrId, baseMonth)) {
                    skipped++;
                    continue;
                }
                long paid = scheduleRepository.sumPaidPrincipal(cntrId);
                long principalBalance = contract.getContractedAmount() - paid;
                if (principalBalance <= 0) {
                    skipped++;
                    continue;
                }

                String dlqStage = delinquencyRepository
                        .findByCntrIdAndDlqStatusCdAndDeletedAtIsNull(cntrId, Delinquency.STATUS_ACTIVE)
                        .map(Delinquency::getDlqStageCd)
                        .orElse(null);

                boolean hasGins = guaranteeInsuranceRepository
                        .existsByCntrIdAndGinsStatusCdAndDeletedAtIsNull(cntrId, GuaranteeInsurance.STATUS_ISSUED);

                EclEngine.EclResult result = eclEngine.calculate(new EclEngine.EclInput(
                        cntrId, principalBalance, dlqStage, hasGins, false));

                eclRepository.save(LoanEclSummary.builder()
                        .cntrId(cntrId)
                        .summaryMonth(baseMonth)
                        .ifrsStageCd(result.ifrsStageCd())
                        .pdBps(result.pdBps())
                        .lgdBps(result.lgdBps())
                        .ead(result.ead())
                        .ecl(result.ecl())
                        .engineVersion(result.engineVersion())
                        .calculatedAt(now)
                        .build());

                totalEcl += result.ecl();
                processed++;
            } catch (RuntimeException e) {
                log.warn("[ecl] cntrId={} baseMonth={} 실패: {}",
                        contract.getCntrId(), baseMonth, e.toString());
                skipped++;
            }
        }

        log.info("[ecl] baseMonth={} total={} processed={} skipped={} totalEcl={}",
                baseMonth, active.size(), processed, skipped, totalEcl);

        return EclCalculationRunResponse.of(baseMonth, active.size(), processed, skipped, totalEcl);
    }
}
