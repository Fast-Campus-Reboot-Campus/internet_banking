package com.bank.loan.accrual.dto;

import com.bank.loan.accrual.domain.InterestAccrual;

import java.time.OffsetDateTime;

public record InterestAccrualResponse(
        Long iaccId,
        Long cntrId,
        String accrualDate,
        Long principalBalance,
        Integer appliedRateBps,
        String dayCountBasisCd,
        Long dailyInterestAmt,
        Long cumulativeInterestAmt,
        String iaccStatusCd,
        OffsetDateTime accruedAt
) {
    public static InterestAccrualResponse of(InterestAccrual a) {
        return new InterestAccrualResponse(
                a.getIaccId(), a.getCntrId(),
                a.getAccrualDate(),
                a.getPrincipalBalance(), a.getAppliedRateBps(),
                a.getDayCountBasisCd(),
                a.getDailyInterestAmt(), a.getCumulativeInterestAmt(),
                a.getIaccStatusCd(), a.getAccruedAt()
        );
    }
}
