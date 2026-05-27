package com.bank.loan.dsr.dto;

import com.bank.loan.dsr.domain.DsrCalculation;

import java.time.OffsetDateTime;

public record DsrCalculationResponse(
        Long dsrId,
        Long applId,
        Long customerId,
        Long annualIncomeAmt,
        Long existingPrincipalTotal,
        Long existingAnnualRepayAmt,
        Long newAnnualRepayAmt,
        Long totalAnnualRepayAmt,
        Integer dsrRatioBps,
        Integer dsrLimitBps,
        String dsrStatusCd,
        String dsrRegTypeCd,
        OffsetDateTime calculatedAt,
        String calcEngineVersion,
        String dsrDetail
) {
    public static DsrCalculationResponse of(DsrCalculation d) {
        return new DsrCalculationResponse(
                d.getDsrId(), d.getApplId(), d.getCustomerId(),
                d.getAnnualIncomeAmt(),
                d.getExistingPrincipalTotal(), d.getExistingAnnualRepayAmt(),
                d.getNewAnnualRepayAmt(), d.getTotalAnnualRepayAmt(),
                d.getDsrRatioBps(), d.getDsrLimitBps(),
                d.getDsrStatusCd(), d.getDsrRegTypeCd(),
                d.getCalculatedAt(), d.getCalcEngineVersion(),
                d.getDsrDetail()
        );
    }
}
