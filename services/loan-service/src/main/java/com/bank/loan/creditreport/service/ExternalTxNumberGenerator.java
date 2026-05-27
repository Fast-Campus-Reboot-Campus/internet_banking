package com.bank.loan.creditreport.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 외부 거래번호 생성기. 포맷: TX-yyyyMMdd-{12자리 hex}.
 * 외부 기관(KCB/NICE) 전송 시 idempotency·추적 키로 사용.
 */
@Component
public class ExternalTxNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(OffsetDateTime at) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "TX-" + at.format(DATE) + "-" + suffix;
    }
}
