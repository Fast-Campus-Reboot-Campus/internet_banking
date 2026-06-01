package com.bank.loan.security;

/**
 * 응답 DTO 에 적용할 PII 노출 수준.
 *
 * 판정 기준: LoanActorContext.piiLevel(application, review)
 * 상세 매트릭스: docs/plan/loan-review-authorization-plan.md §3.3, §8
 */
public enum PiiLevel {

    /**
     * 담당자·심사자(라인) — 주민번호 포함 전체 노출.
     * OPS/INTERNAL/ADMIN 도 동일.
     */
    FULL,

    /**
     * 승인자·지점장·본사·감사 — 직접 식별자(주민번호·전화·계좌) 마스킹.
     * 결정정보(소득·DSR·승인금액)는 결재에 필요하므로 노출 유지.
     */
    MASKED,

    /**
     * 고객 본인 또는 비인가 경로 — 직접 식별자 제거, 금액대 문자열만.
     * break-glass 요약 접근도 이 수준 이하.
     */
    REDACTED
}
