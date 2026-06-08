package com.bank.customer.cert.config;

import com.bank.customer.cert.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 데모 인증서(COMMON/FINCERT/AXFUL-TEST)의 cert_pin_hash 가 NULL 로 남아
 * 웹 6자리 PIN 로그인이 실패하는 문제를 보정한다.
 * V10 마이그레이션은 최초 기동(빈 DB)에서, 이 러너는 이미 기동된 데모 DB에서 PIN 을 채운다.
 * EmployeeAccountSeeder 와 동일하게 local 프로파일에서만 동작한다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class CertificatePinSeedBackfill implements ApplicationRunner {

    private static final String SEEDED_CERT_PIN = "123456";
    private static final List<String> SEEDED_SERIALS = List.of(
            "COMMON-TEST-2024-000001",
            "FINCERT-TEST-2024-000001",
            "AXFUL-TEST-2024-000001"
    );

    private final CertificateRepository certificateRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String pinHash = passwordEncoder.encode(SEEDED_CERT_PIN);

        for (String serial : SEEDED_SERIALS) {
            certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull(serial)
                    .ifPresent(cert -> {
                        cert.updatePinHash(pinHash);
                        log.info("[CertificatePinSeedBackfill] seeded PIN hash for {}", serial);
                    });
        }
    }
}
