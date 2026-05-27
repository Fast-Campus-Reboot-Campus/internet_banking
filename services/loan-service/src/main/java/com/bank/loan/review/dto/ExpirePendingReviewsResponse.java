package com.bank.loan.review.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 본심사 권고 만료 배치 응답.
 *
 *   processed     — 만료된 권고 개수
 *   expiredRevIds — 만료된 revId 목록 (작은 순)
 *   cutoffAt      — 만료 기준 시각 (이 시각 이전 reviewedAt 인 PENDING 권고가 대상)
 */
public record ExpirePendingReviewsResponse(
        int processed,
        List<Long> expiredRevIds,
        OffsetDateTime cutoffAt
) {
}
