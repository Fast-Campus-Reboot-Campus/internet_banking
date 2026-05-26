package com.bank.loan.application.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 대출 신청번호 생성기. 포맷: A + yyyyMMdd + 6자리 랜덤 (총 15자).
 *
 * 충돌 가능성은 낮으나 appl_no UNIQUE 제약으로 DB 가 최종 방어한다.
 * 추후 시퀀스/Redis INCR 기반으로 교체 검토.
 */
@Component
public class ApplicationNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(OffsetDateTime at) {
        int random = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("A%s%06d", at.format(DATE), random);
    }
}
