package com.bank.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// JPA·DataSource auto-configure 활성화 (RAG 도입으로 ai-db 연결 필요)
@SpringBootApplication(scanBasePackages = {"com.bank.ai", "com.bank.common"})
@ConfigurationPropertiesScan("com.bank.ai")
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
