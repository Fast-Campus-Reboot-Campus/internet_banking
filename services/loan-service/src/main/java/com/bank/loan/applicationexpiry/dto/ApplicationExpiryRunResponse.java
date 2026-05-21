package com.bank.loan.applicationexpiry.dto;

import java.time.OffsetDateTime;

/**
 * 승인 만료 일배치 결과.
 *
 *   baseDate            기준일 YYYYMMDD
 *   threshold           이 시각 이전에 승인된(approved_at) 건이 만료 대상
 *   totalCandidates     조회된 만료 후보 수
 *   processed           실제 EXPIRED 로 전이된 건수
 */
public record ApplicationExpiryRunResponse(
        String baseDate,
        OffsetDateTime threshold,
        int totalCandidates,
        int processed
) {
    public static ApplicationExpiryRunResponse of(String baseDate, OffsetDateTime threshold,
                                                  int total, int processed) {
        return new ApplicationExpiryRunResponse(baseDate, threshold, total, processed);
    }
}
