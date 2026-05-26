package com.bank.common.security.jwt;

import java.util.List;

public record JwtClaims(
        Long customerId,
        String email,
        List<String> roles,
        TokenType tokenType
) {}
