package com.bank.loan.creditevaluation.event;

/**
 * 신용평가 완료 시 발행 — DSR 자동 산출 트리거용.
 */
public record CreditEvaluationCompletedEvent(
        Long applId,
        Long customerId,
        String cevalDecisionCd,
        Long annualIncomeAmt
) {}
