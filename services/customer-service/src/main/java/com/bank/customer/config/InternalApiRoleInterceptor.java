package com.bank.customer.config;

import com.bank.common.security.BankRole;
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
 * <p>직무별 실인가는 {@link SecurityConfig} 의 {@code requestMatchers(...).hasAnyRole(...)} 가
 * 담당한다(필터 단계라 이 인터셉터보다 먼저 실행됨). 이 인터셉터는 그보다 앞단에서 "고객
 * 토큰" 같은 비직원 호출을 구조화된 {@code COMMON_403} 으로 끊는 coarse 방어선(defense-in-depth)
 * 으로 남는다 — SecurityConfig 가 더 좁으므로 권한 판정의 단일 소스는 SecurityConfig 다.
 *
 * <p>고객 토큰은 {@code ROLE_CUSTOMER} 만 보유하므로 직원 역할 화이트리스트로 검증한다.
 */
@Component
public class InternalApiRoleInterceptor implements HandlerInterceptor {

    private static final String ROLE_HEADER = "X-User-Role";

    /** 직원 역할 화이트리스트 — SecurityConfig 와 동일한 단일 소스({@link BankRole#EMPLOYEE_ROLES}) */
    private static final Set<String> EMPLOYEE_ROLES = BankRole.employeeAuthorities();

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
