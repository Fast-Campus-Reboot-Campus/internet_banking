package com.bank.loan.creditevaluation.dto;

import com.bank.loan.creditevaluation.domain.CreditEvaluation;

import java.time.OffsetDateTime;

public record CreditEvaluationResponse(
        Long cevalId,
        Long applId,
        Long customerId,
        String cevalEngine,
        String cevalEngineVersion,
        String cevalGrade,
        Integer cevalScore,
        Integer pdBps,
        String cevalDecisionCd,
        Long evalLimitAmount,
        Integer evalRateBps,
        String cevalStatusCd,
        String cevalFactors,
        OffsetDateTime evaluatedAt
) {
    public static CreditEvaluationResponse of(CreditEvaluation e) {
        return new CreditEvaluationResponse(
                e.getCevalId(), e.getApplId(), e.getCustomerId(),
                e.getCevalEngine(), e.getCevalEngineVersion(),
                e.getCevalGrade(), e.getCevalScore(), e.getPdBps(),
                e.getCevalDecisionCd(),
                e.getEvalLimitAmount(), e.getEvalRateBps(),
                e.getCevalStatusCd(), e.getCevalFactors(),
                e.getEvaluatedAt()
        );
    }
}
