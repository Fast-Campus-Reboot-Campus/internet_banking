package com.bank.loan.security;

/**
 * API Gateway 가 주입한 직원 속성(지점·직급).
 * GatewayHeaderAuthFilter 가 Authentication.details 에 설정하고,
 * 서비스 계층에서 (auth.getDetails() instanceof GatewayAuthDetails d) 형태로 꺼낸다.
 */
public record GatewayAuthDetails(
        String branch,  // X-User-Branch 헤더값 (nullable)
        String grade    // X-User-Grade  헤더값 (nullable)
) {}
