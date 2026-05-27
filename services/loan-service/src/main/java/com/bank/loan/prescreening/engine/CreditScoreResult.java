package com.bank.loan.prescreening.engine;

/**
 * 외부 신용평가(가심사) 엔진 호출 응답.
 *
 *   decision           — PASS | REJECT
 *   score              — 신용점수(0-1000)
 *   grade              — 신용등급(AAA ~ D)
 *   pdBps              — 부도확률 (basis points, 0-10000)
 *   estimatedLimitAmt  — 추정 한도 (PASS 시), REJECT 시 null
 *   rejectReasonCd     — 거절 사유 (REJECT 시), PASS 시 null
 *   engineVersion      — 엔진/모델 버전 식별자 (audit · A/B 테스트용)
 */
public record CreditScoreResult(
        String decision,
        Integer score,
        String grade,
        Integer pdBps,
        Long estimatedLimitAmt,
        String rejectReasonCd,
        String engineVersion
) {
    public static final String DECISION_PASS   = "PASS";
    public static final String DECISION_REJECT = "REJECT";

    public boolean isPass() {
        return DECISION_PASS.equals(decision);
    }
}
