package com.bank.customer.customer.dto;

import java.util.List;

/**
 * 가입 현황 통계 — 가입 대시보드(/admin/join-stats)의 집계 데이터원.
 *
 * <p>customer 테이블 집계만 제공한다: 총원·오늘/이번달 가입 완료 수, 상태/등급/채널별 분포.
 * 가입 퍼널(단계별 진입·이탈)·신청/거절률은 신청 추적 테이블이 없어 제외한다 — 별도 도메인 필요.
 */
public record JoinStatsResponse(
        long total,
        long joinedToday,
        long joinedThisMonth,
        List<CodeCount> byStatus,
        List<CodeCount> byGrade,
        List<CodeCount> byChannel
) {
    /** 코드별 집계 한 행. code가 null이면 미분류(미설정 등급/채널). */
    public record CodeCount(String code, long count) {}
}
