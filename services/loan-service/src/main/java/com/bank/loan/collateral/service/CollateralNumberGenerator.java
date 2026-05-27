package com.bank.loan.collateral.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 담보 노출번호 생성기. 포맷: C + yyyyMMdd + 6자리 랜덤 (총 15자).
 * col_no UNIQUE 제약으로 DB 가 최종 방어.
 */
@Component
public class CollateralNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(OffsetDateTime at) {
        int random = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("C%s%06d", at.format(DATE), random);
    }
}
