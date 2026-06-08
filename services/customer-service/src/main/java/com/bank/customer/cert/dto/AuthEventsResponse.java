package com.bank.customer.cert.dto;

/**
 * 인증 이벤트 요약 — Fraud Investigation Agent 의 get_auth_events 도구 응답.
 * 계정탈취(H2) 판별 신호. 읽기 전용.
 *
 * @param customerId               고객 ID
 * @param windowHours              집계 구간(시간)
 * @param recentCertFail           구간 내 인증서 로그인 실패 횟수 (certificate_use 기준)
 * @param passwordChangedRecently  최근 비밀번호/PIN 변경 여부 (※ 현재 미연동 — 추후 wiring)
 */
public record AuthEventsResponse(
        Long customerId,
        int windowHours,
        long recentCertFail,
        boolean passwordChangedRecently
) {}
