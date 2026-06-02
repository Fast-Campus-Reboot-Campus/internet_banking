package com.bank.loan.security;

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

    static final String HEADER_USER_ID     = "X-User-Id";
    static final String HEADER_USER_ROLE   = "X-User-Role";
    static final String HEADER_USER_BRANCH = "X-User-Branch";
    static final String HEADER_USER_GRADE  = "X-User-Grade";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userIdHeader = request.getHeader(HEADER_USER_ID);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long customerId = Long.parseLong(userIdHeader.trim());

                // 콤마 구분 멀티롤 파싱
                String roleHeader = request.getHeader(HEADER_USER_ROLE);
                List<SimpleGrantedAuthority> authorities;
                if (roleHeader != null && !roleHeader.isBlank()) {
                    authorities = java.util.Arrays.stream(roleHeader.split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                } else {
                    authorities = List.of();
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(customerId, null, authorities);

                // 직원 속성(지점·직급)을 details 에 보관 — Stage 4 스코프 판정에서 사용
                // 게이트웨이가 빈 문자열로 set할 수 있으므로 blank → null 정규화
                String branchRaw = request.getHeader(HEADER_USER_BRANCH);
                String gradeRaw  = request.getHeader(HEADER_USER_GRADE);
                String branch = (branchRaw != null && !branchRaw.isBlank()) ? branchRaw : null;
                String grade  = (gradeRaw  != null && !gradeRaw.isBlank())  ? gradeRaw  : null;
                auth.setDetails(new GatewayAuthDetails(branch, grade));

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (NumberFormatException ignored) {
                // X-User-Id 가 숫자 형식이 아님 — 인증 미설정, 이후 Security 에서 401/403 처리
            }
        }

        filterChain.doFilter(request, response);
    }
}
