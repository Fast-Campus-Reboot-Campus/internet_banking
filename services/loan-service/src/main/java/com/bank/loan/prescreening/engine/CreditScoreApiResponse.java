package com.bank.loan.prescreening.engine;

/**
 * 외부 신용평가사 HTTP API 응답 페이로드. {@link CreditScoreResult} 로 매핑되어 도메인에 노출된다.
 *
 *   decision      — PASS | REJECT
 *   score         — 신용점수
 *   grade         — 신용등급
 *   pdBps         — 부도확률(bps)
 *   limitAmount   — 추정 한도 (PASS 시)
 *   rejectReason  — 거절 사유 (REJECT 시)
 *   engineVersion — 외부 엔진 모델 버전 (예: "KCB-2.4")
 */
public record CreditScoreApiResponse(
        String decision,
        Integer score,
        String grade,
        Integer pdBps,
        Long limitAmount,
        String rejectReason,
        String engineVersion
) {
    public CreditScoreResult toResult() {
        return new CreditScoreResult(
                decision, score, grade, pdBps,
                limitAmount, rejectReason, engineVersion
        );
    }
}
