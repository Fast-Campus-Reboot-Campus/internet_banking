package com.bank.customer;

import com.bank.customer.login.config.EmployeeDirectoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bank.customer", "com.bank.common"})
@EntityScan(basePackages = {"com.bank.customer", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.customer", "com.bank.common"})
@EnableConfigurationProperties(EmployeeDirectoryProperties.class)
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
