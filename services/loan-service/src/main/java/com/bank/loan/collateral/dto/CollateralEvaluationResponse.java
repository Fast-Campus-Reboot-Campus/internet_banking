package com.bank.loan.collateral.dto;

import com.bank.loan.collateral.domain.CollateralEvaluation;

import java.time.OffsetDateTime;

public record CollateralEvaluationResponse(
        Long cevalColId,
        Long colId,
        String evalMethodCd,
        String evalAgencyCd,
        Long appraisedValue,
        Long appliedValue,
        String evalStatusCd,
        String evalReportUrl,
        String evalReportHash,
        OffsetDateTime evaluatedAt,
        String appliedStartDate,
        String appliedEndDate
) {
    public static CollateralEvaluationResponse of(CollateralEvaluation e) {
        return new CollateralEvaluationResponse(
                e.getCevalColId(), e.getColId(),
                e.getEvalMethodCd(), e.getEvalAgencyCd(),
                e.getAppraisedValue(), e.getAppliedValue(),
                e.getEvalStatusCd(), e.getEvalReportUrl(), e.getEvalReportHash(),
                e.getEvaluatedAt(),
                e.getAppliedStartDate(), e.getAppliedEndDate()
        );
    }
}
