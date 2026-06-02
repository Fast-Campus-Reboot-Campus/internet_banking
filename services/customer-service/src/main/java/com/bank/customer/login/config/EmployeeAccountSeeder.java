package com.bank.customer.login.config;

import com.bank.customer.customer.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class EmployeeAccountSeeder implements ApplicationRunner {

    private static final String EMPLOYEE_PASSWORD = "Employee1234!";

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeDirectoryProperties employeeDirectory;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!employeeDirectory.enabled() || employeeDirectory.employees() == null) return;

        String hash = passwordEncoder.encode(EMPLOYEE_PASSWORD);

        credentialRepository.findByLoginIdAndDeletedAtIsNull("employee01")
                .ifPresent(c -> {
                    c.changePassword(hash);
                    log.info("[EmployeeSeeder] employee01 password hash updated");
                });

        credentialRepository.findByLoginIdAndDeletedAtIsNull("employee02")
                .ifPresent(c -> {
                    c.changePassword(hash);
                    log.info("[EmployeeSeeder] employee02 password hash updated");
                });
    }
}
