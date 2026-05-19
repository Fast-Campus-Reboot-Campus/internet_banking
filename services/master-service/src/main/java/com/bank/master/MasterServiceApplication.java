package com.bank.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bank.master", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.master", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.master", "com.bank.common"})
public class MasterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MasterServiceApplication.class, args);
    }
}
