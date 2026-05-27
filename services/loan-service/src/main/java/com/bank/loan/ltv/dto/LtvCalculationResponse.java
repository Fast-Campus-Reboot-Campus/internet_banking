package com.bank.loan.ltv.dto;

import com.bank.loan.ltv.domain.LtvCalculation;

import java.time.OffsetDateTime;

public record LtvCalculationResponse(
        Long ltvId,
        Long applId,
        Long colId,
        Long appliedColValue,
        Long seniorLienAmount,
        Long requestedAmount,
        Integer ltvRatioBps,
        Integer ltvLimitBps,
        Long maxLoanAmount,
        String ltvStatusCd,
        OffsetDateTime calculatedAt,
        String calcEngineVersion
) {
    public static LtvCalculationResponse of(LtvCalculation l) {
        return new LtvCalculationResponse(
                l.getLtvId(), l.getApplId(), l.getColId(),
                l.getAppliedColValue(), l.getSeniorLienAmount(), l.getRequestedAmount(),
                l.getLtvRatioBps(), l.getLtvLimitBps(),
                l.getMaxLoanAmount(), l.getLtvStatusCd(),
                l.getCalculatedAt(), l.getCalcEngineVersion()
        );
    }
}
