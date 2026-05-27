package com.bank.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bank.customer", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.customer", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.customer", "com.bank.common"})
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
