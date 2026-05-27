package com.bank.loan.contract.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 계약번호 생성기. 포맷: L + yyyyMMdd + 6자리 랜덤 (총 15자).
 * cntr_no UNIQUE 제약으로 DB 가 최종 방어.
 */
@Component
public class ContractNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(OffsetDateTime at) {
        int random = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("L%s%06d", at.format(DATE), random);
    }
}
