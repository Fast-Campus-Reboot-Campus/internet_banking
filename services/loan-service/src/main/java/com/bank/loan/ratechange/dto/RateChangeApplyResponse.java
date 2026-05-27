package com.bank.loan.ratechange.dto;

import com.bank.loan.ratechange.domain.RateChangeHistory;

import java.time.OffsetDateTime;

public record RateChangeApplyResponse(
        Long rchgId,
        Long cntrId,
        Integer previousRateBps,
        Integer newRateBps,
        String appliedStartDate,
        String rateChangeReasonCd,
        String newScheduleVersionCd,
        int supersededInstallments,
        int newInstallments,
        OffsetDateTime changedAt
) {
    public static RateChangeApplyResponse of(RateChangeHistory h, String newVersion, int superseded, int newCount) {
        return new RateChangeApplyResponse(
                h.getRchgId(), h.getCntrId(),
                h.getPreviousRateBps(), h.getNewRateBps(),
                h.getAppliedStartDate(), h.getRateChangeReasonCd(),
                newVersion, superseded, newCount,
                h.getChangedAt()
        );
    }
}
