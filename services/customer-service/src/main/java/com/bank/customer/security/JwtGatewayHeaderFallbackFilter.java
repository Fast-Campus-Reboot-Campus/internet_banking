package com.bank.customer.security;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway 를 거치지 않고 프론트가 customer-service 를 직접 호출하는 로컬 개발용 JWT 폴백 필터.
 *
 * <p>운영에서는 API Gateway 가 JWT 검증 후 X-User-Id / X-Customer-Id 등 내부 헤더를 주입한다.
 * 로컬에서는 게이트웨이가 없어 프론트가 Bearer 토큰만 보내므로, 이 필터가 토큰을 파싱해
 *   1) SecurityContext 인증 정보를 등록하고
 *   2) 다운스트림 컨트롤러가 {@code @RequestHeader("X-Customer-Id")} 로 읽는 게이트웨이 헤더를
 *      토큰에서 복원해 요청에 주입한다(요청 래퍼).
 *
 * <p>X-User-Id 헤더가 이미 있으면(게이트웨이 경유) 아무것도 하지 않는다.
 */
public class JwtGatewayHeaderFallbackFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID     = "X-User-Id";
    private static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    private static final String HEADER_USER_ROLE   = "X-User-Role";
    private static final String HEADER_USER_BRANCH = "X-User-Branch";
    private static final String HEADER_USER_GRADE  = "X-User-Grade";

    private final JwtProvider jwtProvider;

    public JwtGatewayHeaderFallbackFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 게이트웨이 헤더가 이미 있으면 GatewayHeaderAuthFilter 가 처리했으므로 패스
        if (request.getHeader(HEADER_USER_ID) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = jwtProvider.parseClaims(auth.substring(BEARER_PREFIX.length()));
            if (claims.tokenType() != TokenType.ACCESS) {
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(claims.customerId(), null, authorities);
            authentication.setDetails(new GatewayAuthDetails(claims.branch(), claims.grade()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 게이트웨이가 주입했어야 할 내부 헤더를 토큰에서 복원
            Map<String, String> injected = new LinkedHashMap<>();
            injected.put(HEADER_USER_ID,     String.valueOf(claims.customerId()));
            injected.put(HEADER_CUSTOMER_ID, String.valueOf(claims.customerId()));
            if (claims.roles() != null && !claims.roles().isEmpty()) {
                injected.put(HEADER_USER_ROLE, String.join(",", claims.roles()));
            }
            if (claims.branch() != null) injected.put(HEADER_USER_BRANCH, claims.branch());
            if (claims.grade()  != null) injected.put(HEADER_USER_GRADE,  claims.grade());

            filterChain.doFilter(new HeaderInjectingRequestWrapper(request, injected), response);
        } catch (Exception ignored) {
            // 토큰 파싱 실패 → 인증 미설정, 헤더 미주입 (이후 Security/컨트롤러가 거부)
            filterChain.doFilter(request, response);
        }
    }

    /** 원본 요청에 일부 헤더를 덮어씌워 노출하는 래퍼. */
    private static final class HeaderInjectingRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> extra;

        HeaderInjectingRequestWrapper(HttpServletRequest request, Map<String, String> extra) {
            super(request);
            this.extra = extra;
        }

        @Override
        public String getHeader(String name) {
            String v = extra.get(canonical(name));
            return v != null ? v : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String v = extra.get(canonical(name));
            if (v != null) return Collections.enumeration(List.of(v));
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>(extra.keySet());
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String n = original.nextElement();
                if (extra.keySet().stream().noneMatch(k -> k.equalsIgnoreCase(n))) names.add(n);
            }
            return Collections.enumeration(names);
        }

        /** 주입 헤더는 대소문자 무시 매칭 */
        private String canonical(String name) {
            return extra.keySet().stream()
                    .filter(k -> k.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(name);
        }
    }
}
