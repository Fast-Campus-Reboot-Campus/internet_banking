package com.bank.loan.creditreport.outbox;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plan 02 step 3: outbox 상태 머신 단위 검증.
 */
class CreditInfoReportOutboxTest {

    private CreditInfoReportOutbox newPending() {
        return CreditInfoReportOutbox.builder()
                .crptId(1L)
                .status(CreditInfoReportOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(3)
                .nextAttemptAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void markSent_은_SENT_와_sentAt_을_채움() {
        CreditInfoReportOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.now();

        o.markSent(now);

        assertThat(o.getStatus()).isEqualTo("SENT");
        assertThat(o.getSentAt()).isEqualTo(now);
    }

    @Test
    void markFailed_1회_FAILED_attemptNo_1_nextAttemptAt_백오프() {
        CreditInfoReportOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");

        o.markFailed("network timeout", now);

        assertThat(o.getStatus()).isEqualTo("FAILED");
        assertThat(o.getAttemptNo()).isEqualTo(1);
        assertThat(o.getLastError()).isEqualTo("network timeout");
        // 2^1 = 2분
        assertThat(ChronoUnit.MINUTES.between(now, o.getNextAttemptAt())).isEqualTo(2);
    }

    @Test
    void markFailed_2회_백오프_4분() {
        CreditInfoReportOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        o.markFailed("e1", now);
        o.markFailed("e2", now);

        assertThat(o.getAttemptNo()).isEqualTo(2);
        // 2^2 = 4분
        assertThat(ChronoUnit.MINUTES.between(now, o.getNextAttemptAt())).isEqualTo(4);
    }

    @Test
    void maxAttempt_도달시_DEAD_전이() {
        CreditInfoReportOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.now();
        o.markFailed("e", now);
        o.markFailed("e", now);
        o.markFailed("e", now);

        assertThat(o.getAttemptNo()).isEqualTo(3);
        assertThat(o.getStatus()).isEqualTo("DEAD");
    }

    @Test
    void requeue_는_PENDING_으로_초기화() {
        CreditInfoReportOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.now();
        o.markFailed("e", now);
        o.markFailed("e", now);
        o.markFailed("e", now); // DEAD

        o.requeue(now);

        assertThat(o.getStatus()).isEqualTo("PENDING");
        assertThat(o.getAttemptNo()).isZero();
        assertThat(o.getLastError()).isNull();
        assertThat(o.getNextAttemptAt()).isEqualTo(now);
    }

    @Test
    void lastError_500자_초과_절단() {
        CreditInfoReportOutbox o = newPending();
        String huge = "x".repeat(700);
        o.markFailed(huge, OffsetDateTime.now());
        assertThat(o.getLastError()).hasSize(500);
    }
}
