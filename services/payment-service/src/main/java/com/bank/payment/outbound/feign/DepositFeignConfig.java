package com.bank.payment.outbound.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class DepositFeignConfig {

    @Value("${payment.bank-code:A}")
    private String bankCode;

    @Bean
    public RequestInterceptor depositRequestInterceptor() {
        return template -> {
            // 인스턴스 고정 헤더
            template.header("X-Caller-Service", "payment-service");
            template.header("X-Bank-Code", mapBankCode(bankCode));
            template.header("Accept", "application/json");
            // 요청별 추적 ID (매 호출 UUID)
            template.header("X-Request-Id", UUID.randomUUID().toString());
            // ★ Authorization(JWT)은 Stage 5+ 인증 연동 시 추가 (지금은 미박)
            // ★ X-Idempotency-Key는 @RequestHeader로 메서드에서 받음
        };
    }

    // A=004, B=088 (P-029 은행코드 매핑)
    private String mapBankCode(String code) {
        return "B".equalsIgnoreCase(code) ? "088" : "004";
    }
}
