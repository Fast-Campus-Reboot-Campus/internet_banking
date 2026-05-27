package com.bank.loan.prescreening.dto;

import com.bank.loan.prescreening.domain.LoanPrescreening;

import java.time.OffsetDateTime;

public record LoanPrescreeningResponse(
        Long prescId,
        Long applId,
        String prescResultCd,
        Long estimatedLimitAmt,
        Integer estimatedRateBps,
        String estimatedGrade,
        Integer estimatedScore,
        String rejectReasonCd,
        String prescRemark,
        OffsetDateTime prescreenedAt,
        String prescEngineVersion
) {
    public static LoanPrescreeningResponse of(LoanPrescreening p) {
        return new LoanPrescreeningResponse(
                p.getPrescId(), p.getApplId(),
                p.getPrescResultCd(),
                p.getEstimatedLimitAmt(), p.getEstimatedRateBps(),
                p.getEstimatedGrade(), p.getEstimatedScore(),
                p.getRejectReasonCd(), p.getPrescRemark(),
                p.getPrescreenedAt(), p.getPrescEngineVersion()
        );
    }
}
