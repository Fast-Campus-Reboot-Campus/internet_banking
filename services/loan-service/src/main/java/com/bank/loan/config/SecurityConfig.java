package com.bank.loan.config;

import com.bank.loan.security.GatewayHeaderAuthFilter;
import com.bank.loan.security.InternalTokenFilter;
import com.bank.loan.security.LoanRole;
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
 * 엔드포인트 권한 매트릭스 (LoanRole 기준):
 *   /api/internal/loan-reviews/{id}/bias-ops-note  POST  ROLE_OPS           운영팀 편향 메모
 *   /api/internal/eod/**                                 ROLE_OPS           일배치(EOD)
 *   /api/internal/**                                     ROLE_INTERNAL      서비스 간 X-Internal-Token
 *   /api/loan-reviews/{id}/bias-override           POST  ROLE_HQ_REVIEWER   이상거래 본사 담당자 우회 승인
 *   그 외                                                authenticated       조회 스코프는 Stage 4에서 추가
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
                ).hasRole(LoanRole.OPS.spring())
                .requestMatchers(
                        "/api/internal/eod/**"
                ).hasRole(LoanRole.OPS.spring())

                // 서비스 간 내부 엔드포인트 — ROLE_INTERNAL (X-Internal-Token)
                .requestMatchers("/api/internal/**").hasRole(LoanRole.INTERNAL.spring())

                // 이상거래 본사 담당자 편향 우회 승인 — ROLE_HQ_REVIEWER
                .requestMatchers(HttpMethod.POST,
                        "/api/loan-reviews/*/bias-override"
                ).hasRole(LoanRole.HQ_REVIEWER.spring())

                // 자동 심사 (배치/운영) — ROLE_OPS
                .requestMatchers(HttpMethod.POST,
                        "/api/loan-applications/*/review/auto-decide"
                ).hasRole(LoanRole.OPS.spring())

                // 수동 심사 실행·확정·편향확인 — ROLE_DEPUTY_MANAGER 또는 ROLE_OPS
                .requestMatchers(HttpMethod.POST,
                        "/api/loan-applications/*/review",
                        "/api/loan-applications/*/review/confirm",
                        "/api/loan-applications/*/review/acknowledge-bias"
                ).hasAnyRole(LoanRole.DEPUTY_MANAGER.spring(), LoanRole.OPS.spring())

                // 승인자 최종 결재 — ROLE_BRANCH_MANAGER
                .requestMatchers(HttpMethod.POST,
                        "/api/loan-applications/*/review/approver-approve"
                ).hasRole(LoanRole.BRANCH_MANAGER.spring())

                .anyRequest().authenticated()
            );

        return http.build();
    }
}
