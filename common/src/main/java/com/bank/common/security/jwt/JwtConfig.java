package com.bank.common.security.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jwt.secret 프로퍼티가 존재하는 서비스에서만 JwtProvider 빈을 등록한다.
 * api-gateway, customer-service 등 JWT 를 다루는 서비스의 application.yml 에
 * jwt.secret / jwt.access-token-validity / jwt.refresh-token-validity 를 설정한다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnProperty(name = "jwt.secret")
public class JwtConfig {

    @Bean
    public JwtProvider jwtProvider(JwtProperties properties) {
        return new JwtProvider(properties);
    }
}
