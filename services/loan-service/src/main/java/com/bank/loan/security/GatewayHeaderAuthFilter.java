package com.bank.loan.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 게이트웨이가 JWT 검증 후 주입한 X-User-Id / X-User-Role 헤더를 읽어
 * SecurityContext 에 인증 정보를 등록한다.
 *
 * <p>JWT 를 직접 파싱하지 않는다.
 * API Gateway(JwtAuthFilter) 가 단일 검증 지점으로 동작하고,
 * 검증된 사용자 정보를 내부 헤더로 전파한다.
 *
 * <p>@Component 를 붙이지 않는다 — SecurityConfig 에서 @Bean 으로 등록해
 * Spring Boot 의 자동 서블릿 필터 이중 등록을 방지한다.
 */
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    static final String HEADER_USER_ID   = "X-User-Id";
    static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userIdHeader = request.getHeader(HEADER_USER_ID);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long customerId = Long.parseLong(userIdHeader.trim());

                List<SimpleGrantedAuthority> authorities;
                String role = request.getHeader(HEADER_USER_ROLE);
                if (role != null && !role.isBlank()) {
                    authorities = List.of(new SimpleGrantedAuthority(role.trim()));
                } else {
                    authorities = List.of();
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(customerId, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (NumberFormatException ignored) {
                // X-User-Id 가 숫자 형식이 아님 — 인증 미설정, 이후 Security 에서 401/403 처리
            }
        }

        filterChain.doFilter(request, response);
    }
}
