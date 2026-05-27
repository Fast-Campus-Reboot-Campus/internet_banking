package com.bank.loan.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 내부 서비스 간 호출 인증 필터.
 * JWT 필터 이후에 실행되며, SecurityContext 에 인증 정보가 없는 경우에만 동작한다.
 *
 * X-Internal-Token 헤더에 올바른 토큰이 있으면 ROLE_INTERNAL 권한으로 인증 컨텍스트를 설정한다.
 * /api/internal/** 엔드포인트를 호출하는 에이전트·배치 스케줄러 등 서비스 계정이 사용.
 *
 * @Component 를 붙이지 않는다 — SecurityConfig 에서 @Bean 으로 등록한다.
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME    = "X-Internal-Token";
    public static final String ROLE_INTERNAL  = "ROLE_INTERNAL";
    private static final String PRINCIPAL     = "internal-service";

    private final String configuredToken;

    public InternalTokenFilter(String configuredToken) {
        this.configuredToken = configuredToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        boolean notAuthenticated = (existing == null || !existing.isAuthenticated());

        if (notAuthenticated) {
            String token = request.getHeader(HEADER_NAME);
            if (configuredToken != null && configuredToken.equals(token)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        PRINCIPAL, null,
                        List.of(new SimpleGrantedAuthority(ROLE_INTERNAL)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
