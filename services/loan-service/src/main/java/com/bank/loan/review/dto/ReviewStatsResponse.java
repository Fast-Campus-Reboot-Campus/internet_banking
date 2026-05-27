package com.bank.loan.review.dto;

import java.util.Map;

/**
 * 본심사 통계 응답.
 *
 *   from / to        — 요청 기간 (yyyyMMdd, to 는 inclusive — 그 날 까지 포함)
 *   totalCount       — 기간 내 본심사 row 총 개수
 *   byTypeDecision   — "{revTypeCd}_{revDecisionCd}" 키로 카운트 (예: AUTO_APPROVED, MANUAL_REJECTED).
 *                      revDecisionCd null 이면 _NONE.
 *   byStatus         — revStatusCd 별 카운트 (PENDING_APPROVAL / COMPLETED / EXPIRED)
 *   byRejectReason   — REJECTED 결정의 rejectReasonCd 별 카운트
 */
public record ReviewStatsResponse(
        String from,
        String to,
        long totalCount,
        Map<String, Long> byTypeDecision,
        Map<String, Long> byStatus,
        Map<String, Long> byRejectReason
) {
}
