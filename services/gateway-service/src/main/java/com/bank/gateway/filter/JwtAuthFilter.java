package com.bank.gateway.filter;

import com.bank.gateway.jwt.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 글로벌 인증 필터.
 *
 * Authorization: Bearer <token> 헤더를 검증한 뒤,
 * 하위 서비스로 X-User-Id / X-User-Role 헤더를 주입한다.
 * 하위 서비스는 별도 JWT 검증 없이 이 헤더만 신뢰하면 된다.
 *
 * 화이트리스트 경로는 토큰 없이 통과.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> WHITE_LIST = List.of(
            "/actuator",
            "/api/auth",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[JWT] 토큰 없음 — path={}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = parse(token);

            // 검증된 사용자 정보를 하위 서비스에 헤더로 전달
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id",   claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException e) {
            log.warn("[JWT] 검증 실패 — path={}, reason={}", path, e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -1; // LoggingFilter(-2) 다음, 라우팅 전
    }

    // -----------------------------------------------------------------------

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
