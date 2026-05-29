package com.bank.loan.prescreening.event;

/**
 * 가심사 PASS 시 발행 — CB 자동 실행 트리거용.
 */
public record PrescreeningPassedEvent(
        Long applId,
        Integer estimatedScore,
        String estimatedGrade,
        Long estimatedLimitAmt,
        Integer estimatedRateBps,
        String engineVersion
) {}
