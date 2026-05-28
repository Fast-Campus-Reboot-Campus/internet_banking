package com.bank.loan.batch.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EOD 잡 1회 실행 이력.
 *
 *   jobExecutionId   Spring Batch 실행 PK
 *   baseDate         JobParameter "baseDate" (YYYYMMDD)
 *   status           COMPLETED / FAILED / STARTED / STOPPED / ABANDONED / UNKNOWN
 *   exitCode         Spring Batch exit code (COMPLETED / FAILED / ...)
 *   startTime        실행 시작 (UTC LocalDateTime — Spring Batch 5 표준)
 *   endTime          실행 종료 (실행 중이면 null)
 *   durationMs       소요시간 ms (실행 중이면 null)
 *   steps            스텝별 실행 정보
 */
public record EodHistoryResponse(
        Long jobExecutionId,
        String baseDate,
        String status,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs,
        List<StepInfo> steps
) {

    public record StepInfo(
            Long stepExecutionId,
            String stepName,
            String status,
            String exitCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long durationMs,
            String exitDescription
    ) {}
}
