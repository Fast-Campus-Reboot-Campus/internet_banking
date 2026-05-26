package com.bank.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// JPA·DataSource auto-configure 활성화 (RAG 도입으로 ai-db 연결 필요)
// @EntityScan / @EnableJpaRepositories: common 의 StatusHistory · BaseEntity 스캔 (loan-service 와 동일 패턴)
// @EnableScheduling: RAG 정기 인제스트 스케줄러 — 실제 활성은 rag.scheduler.enabled 로 분기
@SpringBootApplication(scanBasePackages = {"com.bank.ai", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.ai", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.ai", "com.bank.common"})
@ConfigurationPropertiesScan("com.bank.ai")
@EnableScheduling
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
