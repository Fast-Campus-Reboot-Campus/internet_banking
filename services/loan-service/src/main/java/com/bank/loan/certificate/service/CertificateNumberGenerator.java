package com.bank.loan.certificate.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 증명서 번호 생성기. 포맷: CERT-yyyyMMdd-{8자리 hex} (총 22자).
 * cert_no UNIQUE 제약으로 DB 가 최종 방어 — 충돌 시 호출자가 재시도.
 */
@Component
public class CertificateNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(OffsetDateTime at) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CERT-" + at.format(DATE) + "-" + suffix;
    }
}
