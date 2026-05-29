package com.bank.loan.prescreening.client;

import java.math.BigDecimal;

/**
 * auto-loan-review POST /api/ai/auto-review/evaluate 응답 DTO.
 *
 * @param track     AI 트랙 분기 결과 (TRACK_1 자동승인 / TRACK_2 자동반려 / TRACK_3 심사원 배정)
 * @param pd        PD 스코어 (0~1)
 * @param rationale 결정 근거 한 줄 요약 (한국어)
 */
public record AutoReviewEvaluateResult(
        String track,
        BigDecimal pd,
        String rationale
) {
    public static final String TRACK_1 = "TRACK_1";
    public static final String TRACK_2 = "TRACK_2";
    public static final String TRACK_3 = "TRACK_3";
}
