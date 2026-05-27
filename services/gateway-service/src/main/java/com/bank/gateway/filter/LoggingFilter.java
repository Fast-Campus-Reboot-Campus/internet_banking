package com.bank.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 요청/응답 로깅 글로벌 필터.
 *
 * 모든 요청에 대해 메서드, 경로, 응답 상태, 처리 시간을 기록한다.
 * JWT 필터(-1) 보다 먼저 실행(-2)되어 인증 실패 요청도 로깅에 포함된다.
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startMs = System.currentTimeMillis();

        log.info("[{}] → {} {}", request.getId(), request.getMethod(), request.getPath());

        return chain.filter(exchange)
                .doFinally(signal -> log.info("[{}] ← {} {} {}ms",
                        request.getId(),
                        exchange.getResponse().getStatusCode(),
                        request.getPath(),
                        System.currentTimeMillis() - startMs));
    }

    @Override
    public int getOrder() {
        return -2; // JWT 필터(-1) 보다 먼저 실행
    }
}
