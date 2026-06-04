package com.bank.customer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityAutoConfigTest {

    /**
     * test 프로파일은 RedisAutoConfiguration 을 제외하므로 StringRedisTemplate 빈이 없다.
     * 전체 컨텍스트를 로드하는 본 테스트를 위해 mock 빈으로 대체한다(컨텍스트 로딩 전용).
     */
    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            // codeCacheManager(common) 가 RedisConnectionFactory 를 요구하므로 mock 제공.
            // pub/sub 리스너는 test 프로파일에서 비활성(code.cache.subscriber.enabled=false).
            return Mockito.mock(RedisConnectionFactory.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("UserDetailsService 자동 설정 제외 — inMemoryUserDetailsManager 빈 미등록")
    void userDetailsServiceNotAutoConfigured() {
        assertThat(context.containsBean("inMemoryUserDetailsManager")).isFalse();
    }
}
