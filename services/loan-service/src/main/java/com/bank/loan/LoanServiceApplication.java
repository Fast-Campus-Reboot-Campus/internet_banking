package com.bank.loan;

import com.bank.loan.advisory.rag.AdvisoryRagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bank.loan", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.loan", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.loan", "com.bank.common"})
@EnableConfigurationProperties(AdvisoryRagProperties.class)
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
