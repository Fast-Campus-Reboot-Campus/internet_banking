package com.bank.loan.ratechange.dto;

import com.bank.loan.ratechange.domain.RateChangeHistory;

import java.time.OffsetDateTime;

public record RateChangeHistoryResponse(
        Long rchgId,
        Long cntrId,
        String rateChangeReasonCd,
        Integer previousRateBps,
        Integer newRateBps,
        Integer baseRateBps,
        Integer spreadBps,
        Integer preferentialRateBps,
        String appliedStartDate,
        String appliedEndDate,
        OffsetDateTime changedAt
) {
    public static RateChangeHistoryResponse of(RateChangeHistory h) {
        return new RateChangeHistoryResponse(
                h.getRchgId(), h.getCntrId(),
                h.getRateChangeReasonCd(),
                h.getPreviousRateBps(), h.getNewRateBps(),
                h.getBaseRateBps(), h.getSpreadBps(), h.getPreferentialRateBps(),
                h.getAppliedStartDate(), h.getAppliedEndDate(),
                h.getChangedAt()
        );
    }
}
