package com.bank.customer.security;

/**
 * API Gateway 가 주입한 직원 속성(지점·직급).
 * GatewayHeaderAuthFilter 가 Authentication.details 에 설정한다.
 *
 * <p>loan-service 의 동명 클래스와 구조가 동일하다 — 추후 common 으로 추출해 공유 예정.
 */
public record GatewayAuthDetails(
        String branch,  // X-User-Branch 헤더값 (nullable)
        String grade    // X-User-Grade  헤더값 (nullable)
) {}
