package com.bank.loan.delinquency.dto;

import com.bank.loan.delinquency.domain.Delinquency;

import java.time.OffsetDateTime;

public record DelinquencyResponse(
        Long dlqId,
        Long cntrId,
        String dlqStatusCd,
        String dlqStartDate,
        String dlqEndDate,
        Integer dlqDays,
        Long dlqPrincipalAmt,
        Long dlqInterestAmt,
        Long dlqTotalAmt,
        Integer overdueRateBps,
        String dlqStageCd,
        OffsetDateTime resolvedAt
) {
    public static DelinquencyResponse of(Delinquency d) {
        return new DelinquencyResponse(
                d.getDlqId(), d.getCntrId(),
                d.getDlqStatusCd(),
                d.getDlqStartDate(), d.getDlqEndDate(),
                d.getDlqDays(),
                d.getDlqPrincipalAmt(), d.getDlqInterestAmt(), d.getDlqTotalAmt(),
                d.getOverdueRateBps(), d.getDlqStageCd(),
                d.getResolvedAt()
        );
    }
}
