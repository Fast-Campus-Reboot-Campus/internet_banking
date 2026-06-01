package com.bank.common.security.jwt;

import java.util.List;

public record JwtClaims(
        Long customerId,
        String email,
        List<String> roles,
        TokenType tokenType,
        String branch,   // nullable: 직원이면 지점 코드, 고객이면 null
        String grade     // nullable: 직원이면 직급 코드, 고객이면 null
) {}
