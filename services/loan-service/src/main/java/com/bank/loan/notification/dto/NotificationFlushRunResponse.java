package com.bank.loan.notification.dto;

/**
 * EOD notification flush 1회 실행 요약.
 *
 *   iterations      dispatch() 호출 횟수
 *   totalProcessed  누적 픽업 row 수
 *   totalSent       누적 성공 row 수
 *   totalFailed     누적 실패 row 수 (재시도 여지)
 *   totalDead       누적 DEAD 전이 row 수
 *   stopReason      DRAINED (백로그 0) / MAX_ITERATIONS / TIMEOUT
 */
public record NotificationFlushRunResponse(
        int iterations,
        int totalProcessed,
        int totalSent,
        int totalFailed,
        int totalDead,
        String stopReason
) {
    public static final String STOP_DRAINED        = "DRAINED";
    public static final String STOP_MAX_ITERATIONS = "MAX_ITERATIONS";
    public static final String STOP_TIMEOUT        = "TIMEOUT";

    public static NotificationFlushRunResponse of(int iterations, int totalProcessed,
                                                  int totalSent, int totalFailed, int totalDead,
                                                  String stopReason) {
        return new NotificationFlushRunResponse(iterations, totalProcessed,
                totalSent, totalFailed, totalDead, stopReason);
    }
}
