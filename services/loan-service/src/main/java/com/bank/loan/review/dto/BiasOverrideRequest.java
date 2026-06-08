package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 편향 BLOCKED 우회 승인 요청.
 *
 * 우회 승인자(overrideBy)는 게이트웨이가 검증해 SecurityContext 에 주입한
 * 인증 토큰(X-User-Id)에서만 가져온다. 요청 바디로는 받지 않는다(위조 방지).
 */
public record BiasOverrideRequest(
        @NotBlank String overrideReason
) {}
