package com.bank.customer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/internal/** 직무별 인가(SecurityConfig + GatewayHeaderAuthFilter) 검증.
 *
 * <p>403 케이스는 보안 필터 단계에서 끊겨 컨트롤러/DB 를 타지 않으므로 인가 매트릭스만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityAutoConfigTest.TestRedisConfig.class)
class InternalApiSecurityTest {

    private static final String FDS_ENDPOINT        = "/api/v1/internal/fds/rules";
    private static final String COMPLIANCE_ENDPOINT = "/api/v1/internal/compliance/sanctioned";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("역할 헤더 없으면 403")
    void noRole_forbidden() throws Exception {
        mockMvc.perform(get(FDS_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("고객 역할이면 403")
    void customerRole_forbidden() throws Exception {
        mockMvc.perform(get(FDS_ENDPOINT)
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FDS 직무(OPS)면 FDS 엔드포인트 통과(200)")
    void fdsRole_allowed() throws Exception {
        mockMvc.perform(get(FDS_ENDPOINT)
                        .header("X-User-Id", "9011")
                        .header("X-User-Role", "ROLE_OPS"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("직무 불일치(TELLER)면 FDS 엔드포인트 403 — 직원이어도 직무별 차단")
    void teller_forbiddenOnFds() throws Exception {
        mockMvc.perform(get(FDS_ENDPOINT)
                        .header("X-User-Id", "9008")
                        .header("X-User-Role", "ROLE_TELLER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("직무 불일치(TELLER)면 컴플라이언스 엔드포인트 403")
    void teller_forbiddenOnCompliance() throws Exception {
        mockMvc.perform(get(COMPLIANCE_ENDPOINT)
                        .header("X-User-Id", "9008")
                        .header("X-User-Role", "ROLE_TELLER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("컴플라이언스 직무면 컴플라이언스 엔드포인트 통과(200)")
    void compliance_allowedOnCompliance() throws Exception {
        mockMvc.perform(get(COMPLIANCE_ENDPOINT)
                        .header("X-User-Id", "9003")
                        .header("X-User-Role", "ROLE_COMPLIANCE"))
                .andExpect(status().isOk());
    }
}
