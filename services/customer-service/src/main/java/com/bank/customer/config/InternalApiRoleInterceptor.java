package com.bank.customer.config;

import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

/**
 * /api/v1/internal/** (직원 전용 관리 API) 인가 인터셉터.
 *
 * <p>SecurityConfig 가 permitAll 이라 customer-service 자체엔 인가 계층이 없다.
 * 게이트웨이가 JWT 에서 주입한 {@code X-User-Role} 에 직원 역할이 없으면 403 으로 차단해
 * 등급/신용등급/FDS 룰 등 고영향 직원 API 에 대한 방어선(defense-in-depth)을 둔다.
 *
 * <p>고객 토큰은 {@code ROLE_CUSTOMER} 만 보유하므로 직원 역할 화이트리스트로 검증한다.
 */
@Component
public class InternalApiRoleInterceptor implements HandlerInterceptor {

    private static final String ROLE_HEADER = "X-User-Role";

    /** 직원 역할 화이트리스트 — 신규 직급 추가 시 갱신 */
    private static final Set<String> EMPLOYEE_ROLES = Set.of(
            "ROLE_BRANCH_MANAGER", "ROLE_DEPUTY_MANAGER", "ROLE_TELLER");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(ROLE_HEADER);
        boolean isEmployee = header != null && Arrays.stream(header.split(","))
                .map(String::trim)
                .anyMatch(EMPLOYEE_ROLES::contains);

        if (!isEmployee) {
            throw new BusinessException(CommonErrorCode.COMMON_403);
        }
        return true;
    }
}
