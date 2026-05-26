package com.bank.customer.security;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 요청당 1회 실행되는 JWT 검증 필터.
 * 유효한 ACCESS 토큰이 있으면 SecurityContext 에 인증 정보를 등록한다.
 * 토큰이 없거나 유효하지 않은 경우 컨텍스트를 설정하지 않고 다음 필터로 넘긴다
 * (이후 Spring Security 가 보호 경로에 대해 401 을 반환).
 *
 * @Component 를 붙이지 않는다 — SecurityConfig 에서 @Bean 으로 등록해
 * Spring Boot 의 자동 서블릿 필터 이중 등록을 방지한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final MeterRegistry meterRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (jwtProvider.validate(token)) {
                JwtClaims claims = jwtProvider.parseClaims(token);

                if (claims.tokenType() == TokenType.ACCESS) {
                    var authorities = claims.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    claims.customerId(), null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else {
                meterRegistry.counter("customer.jwt.invalid").increment();
            }
        }

        filterChain.doFilter(request, response);
    }
}
