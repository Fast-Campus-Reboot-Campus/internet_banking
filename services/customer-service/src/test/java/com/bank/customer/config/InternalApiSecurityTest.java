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
 * /api/v1/internal/** 직원 전용 인가(SecurityConfig + GatewayHeaderAuthFilter) 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityAutoConfigTest.TestRedisConfig.class)
class InternalApiSecurityTest {

    private static final String INTERNAL_ENDPOINT = "/api/v1/internal/fds/rules";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("역할 헤더 없으면 403")
    void noRole_forbidden() throws Exception {
        mockMvc.perform(get(INTERNAL_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("고객 역할이면 403")
    void customerRole_forbidden() throws Exception {
        mockMvc.perform(get(INTERNAL_ENDPOINT)
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("직원 역할이면 통과(200)")
    void employeeRole_allowed() throws Exception {
        mockMvc.perform(get(INTERNAL_ENDPOINT)
                        .header("X-User-Id", "9001")
                        .header("X-User-Role", "ROLE_BRANCH_MANAGER"))
                .andExpect(status().isOk());
    }
}
