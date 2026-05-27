package com.bank.loan.schedule.dto;

import com.bank.loan.schedule.domain.RepaymentSchedule;

public record RepaymentScheduleResponse(
        Long rschId,
        Long cntrId,
        Integer installmentNo,
        String dueDate,
        Long scheduledPrincipal,
        Long scheduledInterest,
        Long scheduledTotal,
        Long remainingBalance,
        Integer appliedRateBps,
        String rschStatusCd,
        String rschVersionCd
) {
    public static RepaymentScheduleResponse of(RepaymentSchedule s) {
        return new RepaymentScheduleResponse(
                s.getRschId(), s.getCntrId(),
                s.getInstallmentNo(), s.getDueDate(),
                s.getScheduledPrincipal(), s.getScheduledInterest(), s.getScheduledTotal(),
                s.getRemainingBalance(),
                s.getAppliedRateBps(),
                s.getRschStatusCd(), s.getRschVersionCd()
        );
    }
}
