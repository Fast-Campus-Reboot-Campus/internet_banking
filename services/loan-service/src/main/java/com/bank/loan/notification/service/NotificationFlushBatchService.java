package com.bank.loan.notification.service;

import com.bank.loan.notification.dto.NotificationDispatchSummary;
import com.bank.loan.notification.dto.NotificationFlushRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * EOD 마감 시점 알림 백로그 일괄 처리.
 *
 * 평상시: NotificationDispatchService.scheduledDispatch() 가 5초마다 폴링.
 * EOD 시점: 그날의 모든 outbox 적재(이자/연체/만기 등) 완료 직후, 잔여 PENDING 을
 *           한 번에 끌어내려 외부 시스템 알림 지연을 최소화한다.
 *
 * 동작:
 *   dispatch() 를 반복 호출 — processed=0 (DRAINED) 또는 안전 가드 도달 시 종료.
 *
 * 안전 가드:
 *   - maxIterations  (loan.notification.flush.max-iterations, 기본 50)
 *   - maxDurationMs  (loan.notification.flush.max-duration-ms, 기본 60000)
 *   둘 중 먼저 도달 → stopReason 으로 반환.
 *
 * 트랜잭션: 본 서비스는 트랜잭션 경계를 가지지 않는다. dispatch() 가 자체 페이지 readonly +
 * row 단위 REQUIRES_NEW writer 를 관리. 본 메서드는 단순 루프.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFlushBatchService {

    private final NotificationDispatchService dispatchService;

    @Value("${loan.notification.flush.max-iterations:50}")
    private int maxIterations;

    @Value("${loan.notification.flush.max-duration-ms:60000}")
    private long maxDurationMs;

    public NotificationFlushRunResponse run() {
        long startMs = System.currentTimeMillis();
        int iterations = 0;
        int totalProcessed = 0, totalSent = 0, totalFailed = 0, totalDead = 0;
        String stopReason = NotificationFlushRunResponse.STOP_DRAINED;

        while (true) {
            if (iterations >= maxIterations) {
                stopReason = NotificationFlushRunResponse.STOP_MAX_ITERATIONS;
                break;
            }
            if (System.currentTimeMillis() - startMs >= maxDurationMs) {
                stopReason = NotificationFlushRunResponse.STOP_TIMEOUT;
                break;
            }

            NotificationDispatchSummary r = dispatchService.dispatch();
            iterations++;
            totalProcessed += r.processed();
            totalSent      += r.sent();
            totalFailed    += r.failed();
            totalDead      += r.dead();

            if (r.processed() == 0) {
                stopReason = NotificationFlushRunResponse.STOP_DRAINED;
                break;
            }
        }

        log.info("[noti-flush] iter={} processed={} sent={} failed={} dead={} stop={}",
                iterations, totalProcessed, totalSent, totalFailed, totalDead, stopReason);

        return NotificationFlushRunResponse.of(
                iterations, totalProcessed, totalSent, totalFailed, totalDead, stopReason);
    }
}
