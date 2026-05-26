package com.bank.loan.batch.dto;

public record EodRunResponse(
        String baseDate,
        String jobStatus,
        Long jobExecutionId,
        String message
) {
    public static EodRunResponse completed(String baseDate, Long jobExecutionId) {
        return new EodRunResponse(baseDate, "COMPLETED", jobExecutionId, "EOD 배치 완료");
    }

    public static EodRunResponse failed(String baseDate, Long jobExecutionId, String reason) {
        return new EodRunResponse(baseDate, "FAILED", jobExecutionId, reason);
    }

    public static EodRunResponse alreadyRun(String baseDate, Long jobExecutionId) {
        return new EodRunResponse(baseDate, "SKIPPED", jobExecutionId, "해당 baseDate 는 이미 처리되었습니다");
    }
}
