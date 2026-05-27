package com.bank.loan.creditreport.dto;

/**
 * dispatch 배치 1회 실행 요약.
 *
 *   processed  픽업한 outbox row 수
 *   sent       어댑터 성공 → SENT 전이
 *   failed     실패 후 재시도 여지 남음
 *   dead       실패 누적이 maxAttempt 도달 → DEAD 전이
 */
public record CreditInfoReportDispatchSummary(
        int processed,
        int sent,
        int failed,
        int dead
) {
    public static CreditInfoReportDispatchSummary of(int processed, int sent, int failed, int dead) {
        return new CreditInfoReportDispatchSummary(processed, sent, failed, dead);
    }
}
