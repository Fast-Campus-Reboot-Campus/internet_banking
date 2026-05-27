package com.bank.loan.guaranteeinsurance.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 보증보험 증권번호 생성기. 포맷: GINS + yyyyMMdd + 6자리 랜덤 (총 18자).
 * gins_policy_no UNIQUE 제약으로 DB 가 최종 방어.
 */
@Component
public class PolicyNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(OffsetDateTime at) {
        int random = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("GINS%s%06d", at.format(DATE), random);
    }
}
