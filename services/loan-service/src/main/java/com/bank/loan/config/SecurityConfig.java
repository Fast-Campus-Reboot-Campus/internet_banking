package com.bank.loan.config;

import com.bank.common.security.jwt.JwtProvider;
import com.bank.loan.security.InternalTokenFilter;
import com.bank.loan.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
 *   1. JwtAuthenticationFilter  — Bearer 토큰 검증 → SecurityContext 에 JWT 인증 정보 등록
 *   2. InternalTokenFilter      — JWT 인증 없는 경우에만 X-Internal-Token 검증 → ROLE_INTERNAL 등록
 *
 * 엔드포인트 권한 정책:
 *   /api/internal/{id}/bias-ops-note  ROLE_OPS        (운영팀 JWT)
 *   /api/internal/eod/...             ROLE_OPS        (운영팀 JWT)
 *   /api/internal/...                 ROLE_INTERNAL   (서비스 간 X-Internal-Token)
 *   /api/loan-reviews/{id}/bias-override ROLE_SENIOR_REVIEWER (상급 심사원 JWT)
 *   그 외                              authenticated  (JWT)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider);
    }

    @Bean
    public InternalTokenFilter internalTokenFilter(
            @Value("${internal.token}") String internalToken) {
        return new InternalTokenFilter(internalToken);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           InternalTokenFilter internalTokenFilter) throws Exception {
        JwtAuthenticationFilter jwtFilter = jwtAuthenticationFilter();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // JWT 먼저, 이후 Internal token (JWT 인증 없을 때만 활성화)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(internalTokenFilter, JwtAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(
                        "/actuator/health", "/actuator/info", "/actuator/prometheus"
                ).permitAll()

                // 운영팀 전용 내부 엔드포인트 — ROLE_OPS (JWT)
                .requestMatchers(HttpMethod.POST,
                        "/api/internal/loan-reviews/*/bias-ops-note"
                ).hasRole("OPS")
                .requestMatchers(
                        "/api/internal/eod/**"
                ).hasRole("OPS")

                // 서비스 간 내부 엔드포인트 — ROLE_INTERNAL (X-Internal-Token)
                .requestMatchers("/api/internal/**").hasRole("INTERNAL")

                // 편향 우회 승인 — ROLE_SENIOR_REVIEWER (JWT)
                .requestMatchers(HttpMethod.POST,
                        "/api/loan-reviews/*/bias-override"
                ).hasRole("SENIOR_REVIEWER")

                .anyRequest().authenticated()
            );

        return http.build();
    }
}
