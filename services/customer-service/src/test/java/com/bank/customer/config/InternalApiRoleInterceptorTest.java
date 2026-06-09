package com.bank.customer.config;

import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * /api/v1/internal/** 직원 인가 인터셉터 검증.
 */
class InternalApiRoleInterceptorTest {

    private final InternalApiRoleInterceptor interceptor = new InternalApiRoleInterceptor();
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    private HttpServletRequest requestWithRole(String roleHeader) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-User-Role")).thenReturn(roleHeader);
        return req;
    }

    @Test
    @DisplayName("직원 역할이면 통과한다")
    void employeeRole_passes() {
        boolean result = interceptor.preHandle(requestWithRole("ROLE_TELLER"), response, new Object());
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("본사 직급(COMPLIANCE 등)도 통과한다 — SecurityConfig 와 화이트리스트 일치")
    void hqRole_passes() {
        assertThat(interceptor.preHandle(requestWithRole("ROLE_COMPLIANCE"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(requestWithRole("ROLE_HQ_RISK"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(requestWithRole("ROLE_HQ_REVIEWER"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(requestWithRole("ROLE_HQ_MARKETING"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(requestWithRole("ROLE_OPS"), response, new Object())).isTrue();
    }

    @Test
    @DisplayName("여러 역할 중 직원 역할이 포함되면 통과한다")
    void mixedRolesWithEmployee_passes() {
        boolean result = interceptor.preHandle(
                requestWithRole("ROLE_CUSTOMER,ROLE_BRANCH_MANAGER"), response, new Object());
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("고객 역할만 있으면 403 으로 차단된다")
    void customerOnly_forbidden() {
        assertThatThrownBy(() ->
                interceptor.preHandle(requestWithRole("ROLE_CUSTOMER"), response, new Object()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.COMMON_403));
    }

    @Test
    @DisplayName("역할 헤더가 없으면 403 으로 차단된다 (deny-by-default)")
    void noHeader_forbidden() {
        assertThatThrownBy(() ->
                interceptor.preHandle(requestWithRole(null), response, new Object()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.COMMON_403));
    }
}
