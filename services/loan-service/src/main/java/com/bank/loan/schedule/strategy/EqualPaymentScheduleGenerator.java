package com.bank.loan.schedule.strategy;

import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.service.EqualPaymentCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 원리금균등(EQUAL) 회차 생성기.
 *
 * 매 회차 납입액(원금+이자) 이 일정하고, 회차가 갈수록 원금 비중이 증가한다.
 * 회차당 금액 계산은 {@link EqualPaymentCalculator} 공식 유틸에 위임 — 라운딩 정책 동일.
 */
@Component
public class EqualPaymentScheduleGenerator implements RepaymentScheduleGenerator {

    public static final String METHOD = "EQUAL";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String supportedMethod() {
        return METHOD;
    }

    @Override
    public List<RepaymentSchedule> generate(LoanContract contract) {
        List<EqualPaymentCalculator.Installment> installments = EqualPaymentCalculator.calculate(
                contract.getContractedAmount(),
                contract.getTotalRateBps(),
                contract.getContractedPeriodMo()
        );

        LocalDate startDate = LocalDate.parse(contract.getCntrStartDate(), DATE);
        List<RepaymentSchedule> rows = new ArrayList<>(installments.size());

        for (int i = 0; i < installments.size(); i++) {
            EqualPaymentCalculator.Installment inst = installments.get(i);
            int installmentNo = i + 1;
            LocalDate dueDate = startDate.plusMonths(installmentNo);

            rows.add(RepaymentSchedule.builder()
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

        return rows;
    }
}
