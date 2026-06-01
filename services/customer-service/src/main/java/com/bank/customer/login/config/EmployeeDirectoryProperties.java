package com.bank.customer.login.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "employee-directory")
public record EmployeeDirectoryProperties(
        boolean enabled,
        List<EmployeeEntry> employees
) {
    public record EmployeeEntry(
            Long customerId,
            String branch,
            String grade,
            List<String> roles
    ) {}

    public Optional<EmployeeEntry> findById(Long customerId) {
        if (!enabled || employees == null) return Optional.empty();
        return employees.stream()
                .filter(e -> e.customerId().equals(customerId))
                .findFirst();
    }
}
