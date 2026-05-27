package com.bank.gateway.filter;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import com.bank.common.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 모든 요청을 가로채 JWT 를 검증한다.
 * 공개 경로는 토큰 없이 통과시키고, 보호 경로는 유효한 ACCESS 토큰이 있어야 upstream 으로 전달한다.
 * 검증 성공 시 X-Customer-Id / X-Customer-Email 헤더를 downstream 에 추가한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    /** 인증 없이 통과할 경로 접두사 목록 */
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/actuator/"
    );

    private final JwtProvider jwtProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        JwtClaims claims;
        try {
            claims = jwtProvider.parseClaims(token);
        } catch (BusinessException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        if (claims.tokenType() != TokenType.ACCESS) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-Customer-Id", String.valueOf(claims.customerId()))
                .header("X-Customer-Email", claims.email() != null ? claims.email() : "")
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
