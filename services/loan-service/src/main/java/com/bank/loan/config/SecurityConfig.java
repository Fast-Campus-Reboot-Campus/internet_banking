package com.bank.loan.config;

import com.bank.loan.security.GatewayHeaderAuthFilter;
import com.bank.loan.security.InternalTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * loan-service Spring Security 설정.
 *
 * 필터 실행 순서:
 *   1. GatewayHeaderAuthFilter — X-User-Id / X-User-Role 헤더 읽기 → SecurityContext 에 인증 정보 등록
 *                                (API Gateway 가 JWT 검증 후 주입한 헤더를 신뢰)
 *   2. InternalTokenFilter     — Gateway 헤더 인증이 없는 경우에만 X-Internal-Token 검증 → ROLE_INTERNAL 등록
 *
 * 엔드포인트 권한 정책:
 *   /api/internal/{id}/bias-ops-note  ROLE_OPS        (운영팀)
 *   /api/internal/eod/...             ROLE_OPS        (운영팀)
 *   /api/internal/...                 ROLE_INTERNAL   (서비스 간 X-Internal-Token)
 *   /api/loan-reviews/{id}/bias-override ROLE_SENIOR_REVIEWER (상급 심사원)
 *   그 외                              authenticated
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GatewayHeaderAuthFilter gatewayHeaderAuthFilter() {
        return new GatewayHeaderAuthFilter();
    }

    @Bean
    public InternalTokenFilter internalTokenFilter(
            @Value("${internal.token}") String internalToken) {
        return new InternalTokenFilter(internalToken);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           InternalTokenFilter internalTokenFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(gatewayHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(internalTokenFilter, GatewayHeaderAuthFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(
                        "/actuator/health", "/actuator/info", "/actuator/prometheus"
                ).permitAll()

                // 운영팀 전용 내부 엔드포인트 — ROLE_OPS
                .requestMatchers(HttpMethod.POST,
                        "/api/internal/loan-reviews/*/bias-ops-note"
                ).hasRole("OPS")
                .requestMatchers(
                        "/api/internal/eod/**"
                ).hasRole("OPS")

                // 서비스 간 내부 엔드포인트 — ROLE_INTERNAL (X-Internal-Token)
                .requestMatchers("/api/internal/**").hasRole("INTERNAL")

                // 편향 우회 승인 — ROLE_SENIOR_REVIEWER
                .requestMatchers(HttpMethod.POST,
                        "/api/loan-reviews/*/bias-override"
                ).hasRole("SENIOR_REVIEWER")

                .anyRequest().authenticated()
            );

        return http.build();
    }
}
