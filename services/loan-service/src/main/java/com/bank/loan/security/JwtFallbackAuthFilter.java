package com.bank.loan.security;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway를 거치지 않고 직접 호출될 때(로컬 개발)를 위한 JWT 폴백 필터.
 * X-User-Id 헤더가 이미 있으면(Gateway 경유) 건너뛴다.
 */
public class JwtFallbackAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtFallbackAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Gateway 헤더가 이미 있으면 GatewayHeaderAuthFilter가 처리했으므로 패스
        if (request.getHeader(GatewayHeaderAuthFilter.HEADER_USER_ID) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            String token = auth.substring(BEARER_PREFIX.length());
            try {
                JwtClaims claims = jwtProvider.parseClaims(token);
                if (claims.tokenType() == TokenType.ACCESS) {
                    List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims.customerId(), null, authorities);
                    authentication.setDetails(new GatewayAuthDetails(claims.branch(), claims.grade()));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // 토큰 파싱 실패 시 인증 미설정 → Security가 403 처리
            }
        }

        filterChain.doFilter(request, response);
    }
}
