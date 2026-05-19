package com.bank.deposit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bank.deposit", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.deposit", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.deposit", "com.bank.common"})
public class DepositServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepositServiceApplication.class, args);
    }
}
