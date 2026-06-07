package com.bank.customer.config;

import com.bank.customer.security.CustomerRole;
import com.bank.customer.security.GatewayHeaderAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * JWT 검증은 api-gateway 에서 수행하고, 검증된 사용자 정보는 X-User-* 헤더로 전파된다.
 *
 * <p>customer-service 는 게이트웨이 헤더를 {@link GatewayHeaderAuthFilter} 로 SecurityContext 에
 * 올린 뒤, 직원 전용 {@code /api/v1/internal/**} 만 역할로 보호한다(고영향 관리 API 방어선).
 * 그 외 경로는 게이트웨이 1차 검증 + 내부망 신뢰로 permitAll 을 유지한다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(EmployeeDirectoryProperties.class)
public class SecurityConfig {

    /** 직원 역할(고객 제외) — /internal 관리 API 접근 허용 대상 */
    private static final String[] EMPLOYEE_ROLES = {
            CustomerRole.TELLER.spring(),
            CustomerRole.DEPUTY_MANAGER.spring(),
            CustomerRole.BRANCH_MANAGER.spring(),
            CustomerRole.HQ_REVIEWER.spring(),
            CustomerRole.COMPLIANCE.spring(),
            CustomerRole.OPS.spring(),
            CustomerRole.ADMIN.spring(),
    };

    @Bean
    public GatewayHeaderAuthFilter gatewayHeaderAuthFilter() {
        return new GatewayHeaderAuthFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(gatewayHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                    // 직원 전용 관리 API — 등급/신용/FDS 룰 등 고영향. 직원 역할 필요
                    .requestMatchers("/api/v1/internal/**").hasAnyRole(EMPLOYEE_ROLES)
                    // 그 외 — 게이트웨이 1차 검증 + 내부망 신뢰
                    .anyRequest().permitAll());

        return http.build();
    }

    /**
     * 로컬 개발에서 프론트(localhost:3000/3001)가 게이트웨이를 거치지 않고
     * customer-service 를 직접 호출(로그인·인증)할 때 필요한 CORS 허용.
     * 운영은 게이트웨이가 CORS 를 처리한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
