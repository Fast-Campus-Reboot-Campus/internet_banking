package com.bank.loan.schedule.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.dto.RepaymentScheduleListResponse;
import com.bank.loan.schedule.dto.RepaymentScheduleResponse;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 상환 스케줄 생성 · 조회 서비스.
 *
 * 생성은 drawdown 의 부수효과로 호출된다 (flows §1.1 CONTRACTED→DISBURSED 부수효과).
 * 본 단계에서는 EQUAL(원리금균등) 만 지원 — 그 외 상환방식은 LOAN_084 로 차단.
 *
 * due_date 는 cntr_start_date 기준 매월 같은 일자(LocalDate.plusMonths 의 month-end 보정 적용).
 * 영업일 보정(BUSINESS_CALENDAR) 은 본 단계 범위 밖 — 후속 작업.
 *
 * 회차당 applied_rate_bps 는 약정의 total_rate_bps 그대로 적용.
 * 금리 변경 시 신규 버전(V2, V3 ...)으로 재생성하고 기존 행은 SUPERSEDED 로 전이한다 — flows §2.3.
 */
@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {

    public static final String REPAY_METHOD_EQUAL = "EQUAL";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RepaymentScheduleRepository repository;
    private final LoanContractRepository contractRepository;

    /**
     * 최초 인출 시점에 호출. 이미 스케줄이 존재하면 멱등 처리 (재호출 시 no-op).
     * 비EQUAL 상환방식은 LOAN_084 throw.
     */
    @Transactional
    public List<RepaymentSchedule> generateForFirstDrawdown(LoanContract contract) {
        if (repository.existsByCntrIdAndDeletedAtIsNull(contract.getCntrId())) {
            return List.of();
        }

        if (!REPAY_METHOD_EQUAL.equals(contract.getRepaymentMethodCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_084,
                    "repaymentMethodCd=" + contract.getRepaymentMethodCd());
        }

        List<EqualPaymentCalculator.Installment> installments = EqualPaymentCalculator.calculate(
                contract.getContractedAmount(),
                contract.getTotalRateBps(),
                contract.getContractedPeriodMo()
        );

        LocalDate startDate = LocalDate.parse(contract.getCntrStartDate(), DATE);

        List<RepaymentSchedule> toSave = new ArrayList<>(installments.size());
        for (int i = 0; i < installments.size(); i++) {
            EqualPaymentCalculator.Installment inst = installments.get(i);
            int installmentNo = i + 1;
            LocalDate dueDate = startDate.plusMonths(installmentNo);

            toSave.add(RepaymentSchedule.builder()
                    .cntrId(contract.getCntrId())
                    .installmentNo(installmentNo)
                    .dueDate(dueDate.format(DATE))
                    .scheduledPrincipal(inst.scheduledPrincipal())
                    .scheduledInterest(inst.scheduledInterest())
                    .scheduledTotal(inst.scheduledTotal())
                    .remainingBalance(inst.remainingBalance())
                    .appliedRateBps(contract.getTotalRateBps())
                    .rschStatusCd(RepaymentSchedule.STATUS_DUE)
                    .rschVersionCd(RepaymentSchedule.VERSION_INITIAL)
                    .build());
        }

        return repository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public RepaymentScheduleListResponse listLatest(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        List<RepaymentScheduleResponse> items = repository
                .findByCntrIdAndRschVersionCdAndDeletedAtIsNullOrderByInstallmentNoAsc(
                        cntrId, RepaymentSchedule.VERSION_INITIAL)
                .stream()
                .map(RepaymentScheduleResponse::of)
                .toList();
        return RepaymentScheduleListResponse.of(cntrId, RepaymentSchedule.VERSION_INITIAL, items);
    }
}
