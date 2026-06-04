package com.bank.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

/**
 * 직원 디렉터리 config 시드 (초경량 MVP).
 * customerId → {branch, grade, roles} 매핑을 application.yml 로 관리한다.
 * 직원 수가 늘어나면 DB 테이블로 전환한다.
 */
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

    public Optional<EmployeeEntry> find(Long customerId) {
        if (!enabled || employees == null) return Optional.empty();
        return employees.stream()
                .filter(e -> e.customerId().equals(customerId))
                .findFirst();
    }
}
