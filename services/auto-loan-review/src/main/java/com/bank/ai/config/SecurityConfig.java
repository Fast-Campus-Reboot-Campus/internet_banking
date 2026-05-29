package com.bank.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auto-loan-review Spring Security 설정.
 *
 * <p>이 서비스는 API Gateway 뒤에 위치한다. 외부 사용자 인증(JWT)은
 * gateway-service 의 {@code JwtAuthFilter} 가 강제하므로, 서비스단에서는
 * 엔드포인트를 개방한다.
 *
 * <ul>
 *   <li>{@code /api/ai/**} — gateway 가 인증한 트래픽</li>
 *   <li>{@code /api/internal/embeddings/batch} — {@code EmbeddingBatchController}
 *       가 {@code X-Internal-Token} 헤더를 자체 검증(불일치 시 401)</li>
 *   <li>{@code /actuator/**}, swagger — 운영/문서</li>
 * </ul>
 *
 * <p>spring-security 가 (common 경유) classpath 에 올라오므로, 기본 설정의
 * 폼 로그인 리다이렉트(302)를 막기 위해 명시적 permitAll 체인을 등록한다.
 * 구 ai-service 는 보안 스타터가 없어 무방비로 열려 동작했고, 본 설정은
 * 동일한 접근 정책을 명시적으로 재현한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
