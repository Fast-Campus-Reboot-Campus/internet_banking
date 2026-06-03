package com.bank.loan.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskingUtilTest {

    // ── maskPhone ──────────────────────────────────────────────────────────

    @Test
    void maskPhone_null_반환null() {
        assertThat(PiiMaskingUtil.maskPhone(null)).isNull();
    }

    @Test
    void maskPhone_일반번호_재마스킹() {
        assertThat(PiiMaskingUtil.maskPhone("010-1234-5678")).isEqualTo("010-****-****");
    }

    @Test
    void maskPhone_이미마스킹된값_재마스킹() {
        assertThat(PiiMaskingUtil.maskPhone("010-****-5678")).isEqualTo("010-****-****");
    }

    @Test
    void maskPhone_3자리중간번호_재마스킹() {
        assertThat(PiiMaskingUtil.maskPhone("010-123-4567")).isEqualTo("010-****-****");
    }

    // ── amountRange ────────────────────────────────────────────────────────

    @Test
    void amountRange_null_반환null() {
        assertThat(PiiMaskingUtil.amountRange(null)).isNull();
    }

    @Test
    void amountRange_백만원미만() {
        assertThat(PiiMaskingUtil.amountRange(500_000L)).isEqualTo("100만원 미만");
    }

    @Test
    void amountRange_백만원대() {
        assertThat(PiiMaskingUtil.amountRange(5_200_000L)).isEqualTo("5백만원대");
    }

    @Test
    void amountRange_천만원대() {
        assertThat(PiiMaskingUtil.amountRange(52_000_000L)).isEqualTo("5천만원대");
    }

    @Test
    void amountRange_억원대() {
        assertThat(PiiMaskingUtil.amountRange(150_000_000L)).isEqualTo("1억원대");
    }

    @Test
    void amountRange_경계값_1천만원() {
        assertThat(PiiMaskingUtil.amountRange(10_000_000L)).isEqualTo("1천만원대");
    }

    @Test
    void amountRange_경계값_1억원() {
        assertThat(PiiMaskingUtil.amountRange(100_000_000L)).isEqualTo("1억원대");
    }
}
