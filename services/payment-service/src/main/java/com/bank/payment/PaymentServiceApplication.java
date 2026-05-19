package com.bank.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bank.payment", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.payment", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.payment", "com.bank.common"})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
