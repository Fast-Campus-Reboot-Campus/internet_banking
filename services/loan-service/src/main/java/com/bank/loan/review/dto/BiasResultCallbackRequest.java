package com.bank.loan.review.dto;

/**
 * review-ai-gateway → loan-service 편향 검증 결과 콜백.
 *
 * @param status         PASS / FLAGGED / FAILED
 * @param analysisType   분석 유형 (BIAS_DETECTION 등)
 * @param findingSummary 감사 의견 한 줄 요약
 * @param biasDetected   편향 탐지 여부
 */
public record BiasResultCallbackRequest(
        String status,
        String analysisType,
        String findingSummary,
        boolean biasDetected
) {
    public static final String STATUS_PASS    = "PASS";
    public static final String STATUS_FLAGGED = "FLAGGED";
    public static final String STATUS_FAILED  = "FAILED";
}
