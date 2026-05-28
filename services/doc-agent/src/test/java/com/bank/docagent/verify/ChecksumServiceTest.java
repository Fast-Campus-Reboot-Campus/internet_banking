package com.bank.docagent.verify;

import com.bank.docagent.verify.service.ChecksumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChecksumServiceTest {

    private final ChecksumService sut = new ChecksumService();

    @Test
    @DisplayName("SSN 원본 없으면 SKIP(true) 반환")
    void ssn_null_returns_true() {
        assertThat(sut.validateSsn("900101-1******", null)).isTrue();
    }

    @Test
    @DisplayName("자릿수 불일치 시 false")
    void ssn_wrong_length() {
        assertThat(sut.validateSsn(null, "12345")).isFalse();
    }

    @Test
    @DisplayName("사업자번호 유효 케이스")
    void business_number_valid() {
        // 220-81-10004: 가중합 26, 체크=4, 마지막 자리=4 → 유효
        assertThat(sut.validateBusinessNumber("2208110004")).isTrue();
    }

    @Test
    @DisplayName("사업자번호 무효 케이스")
    void business_number_invalid() {
        assertThat(sut.validateBusinessNumber("1234567890")).isFalse();
    }
}
