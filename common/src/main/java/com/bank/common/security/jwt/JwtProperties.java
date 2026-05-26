package com.bank.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jwt.secret / jwt.access-token-validity / jwt.refresh-token-validity
 * 를 application.yml 에서 바인딩한다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidity,
        long refreshTokenValidity
) {}
