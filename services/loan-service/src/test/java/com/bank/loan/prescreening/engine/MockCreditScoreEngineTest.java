package com.bank.loan.prescreening.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockCreditScoreEngine 단위 테스트 — Spring 의존 없는 POJO 테스트.
 */
class MockCreditScoreEngineTest {

    private final MockCreditScoreEngine engine = new MockCreditScoreEngine();

    @Test
    void PASS_정규직_충분한_소득() {
        CreditScoreResult r = engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isTrue();
        assertThat(r.score()).isEqualTo(720);
        assertThat(r.grade()).isEqualTo("BBB");
        assertThat(r.pdBps()).isEqualTo(1120); // (1000-720)*4
        assertThat(r.estimatedLimitAmt()).isEqualTo(30_000_000L); // min(req, income*5)
        assertThat(r.rejectReasonCd()).isNull();
        assertThat(r.engineVersion()).isEqualTo("MOCK-v1");
    }

    @Test
    void PASS_자영업_낮은_점수() {
        CreditScoreResult r = engine.evaluate(req(60_000_000L, 30_000_000L, "SELF_EMPLOYED"));

        assertThat(r.isPass()).isTrue();
        assertThat(r.score()).isEqualTo(660);
        assertThat(r.grade()).isEqualTo("BB");
    }

    @Test
    void PASS_고용형태_미지정_기본_등급() {
        CreditScoreResult r = engine.evaluate(req(60_000_000L, 30_000_000L, null));

        assertThat(r.isPass()).isTrue();
        assertThat(r.score()).isEqualTo(620);
        assertThat(r.grade()).isEqualTo("B");
    }

    @Test
    void PASS_estimatedLimit_은_신청금액과_연소득5배중_작은값() {
        // income×5 = 50M < requested 100M → cap 50M
        CreditScoreResult r = engine.evaluate(req(10_000_000L, 100_000_000L, "EMPLOYEE"));
        // 사실 이 케이스는 OVER_INCOME_5X 거절. 그래서 한도 cap 시나리오는 income×5 ≥ requested 가 필수
        assertThat(r.isPass()).isFalse();
        assertThat(r.rejectReasonCd()).isEqualTo("OVER_INCOME_5X");

        // 정상 cap 시나리오: income 60M, requested 30M → cap min(30M, 300M) = 30M
        CreditScoreResult r2 = engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE"));
        assertThat(r2.estimatedLimitAmt()).isEqualTo(30_000_000L);

        // 신청금액 미지정 시 → income × 5
        CreditScoreResult r3 = engine.evaluate(req(60_000_000L, null, "EMPLOYEE"));
        assertThat(r3.estimatedLimitAmt()).isEqualTo(300_000_000L);
    }

    @Test
    void REJECT_소득_null() {
        CreditScoreResult r = engine.evaluate(req(null, 30_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isFalse();
        assertThat(r.decision()).isEqualTo("REJECT");
        assertThat(r.rejectReasonCd()).isEqualTo("INCOME_NOT_PROVIDED");
        assertThat(r.score()).isNull();
        assertThat(r.estimatedLimitAmt()).isNull();
    }

    @Test
    void REJECT_소득_0() {
        CreditScoreResult r = engine.evaluate(req(0L, 30_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isFalse();
        assertThat(r.rejectReasonCd()).isEqualTo("INCOME_NOT_PROVIDED");
    }

    @Test
    void REJECT_신청금액_연소득_5배_초과() {
        // income 10M × 5 = 50M, requested 60M → REJECT
        CreditScoreResult r = engine.evaluate(req(10_000_000L, 60_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isFalse();
        assertThat(r.rejectReasonCd()).isEqualTo("OVER_INCOME_5X");
    }

    @Test
    void 같은_입력_같은_출력_idempotent() {
        CreditScoreRequest in = req(60_000_000L, 30_000_000L, "EMPLOYEE");
        CreditScoreResult a = engine.evaluate(in);
        CreditScoreResult b = engine.evaluate(in);
        assertThat(a).isEqualTo(b);
    }

    private CreditScoreRequest req(Long income, Long requested, String emp) {
        return new CreditScoreRequest(
                /*customerId*/ 1001L,
                /*loanTypeCd*/ "CREDIT",
                /*requestedAmount*/ requested,
                /*requestedPeriodMo*/ 36,
                /*loanPurposeCd*/ "LIVING",
                /*employmentTypeCd*/ emp,
                /*estimatedIncomeAmt*/ income
        );
    }
}
