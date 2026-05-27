package com.bank.customer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityAutoConfigTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("UserDetailsService 자동 설정 제외 — inMemoryUserDetailsManager 빈 미등록")
    void userDetailsServiceNotAutoConfigured() {
        assertThat(context.containsBean("inMemoryUserDetailsManager")).isFalse();
    }
}
