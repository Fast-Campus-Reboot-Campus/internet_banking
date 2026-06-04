package com.bank.loan.rag.outbox;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoanReviewOutbox 상태 전이·멱등 키 단위 테스트 — Phase E (E3-4).
 */
class LoanReviewOutboxTest {

    @Test
    void idempotencyKey_는_eventType과_aggregateId_조합() {
        assertThat(LoanReviewOutbox.idempotencyKeyOf("CASE_INDEXED", 100L))
                .isEqualTo("CASE_INDEXED:100");
    }

    @Test
    void caseIndexed_는_PENDING_초기상태() {
        var outbox = LoanReviewOutbox.caseIndexed(100L, "{}");

        assertThat(outbox.getStatus()).isEqualTo(LoanReviewOutbox.STATUS_PENDING);
        assertThat(outbox.getAggregateId()).isEqualTo(100L);
        assertThat(outbox.getAttemptNo()).isZero();
        assertThat(outbox.getMaxAttempt()).isEqualTo(LoanReviewOutbox.DEFAULT_MAX_ATTEMPT);
        assertThat(outbox.getIdempotencyKey()).isEqualTo("CASE_INDEXED:100");
    }

    @Test
    void markSent_는_SENT로_전이하고_sentAt_설정() {
        var outbox = LoanReviewOutbox.caseIndexed(1L, "{}");
        OffsetDateTime now = OffsetDateTime.now();

        outbox.markSent(now);

        assertThat(outbox.getStatus()).isEqualTo(LoanReviewOutbox.STATUS_SENT);
        assertThat(outbox.getSentAt()).isEqualTo(now);
    }

    @Test
    void markFailed_상한_미만이면_FAILED_백오프() {
        var outbox = LoanReviewOutbox.caseIndexed(1L, "{}");
        OffsetDateTime now = OffsetDateTime.now();

        outbox.markFailed("kafka down", now);

        assertThat(outbox.getStatus()).isEqualTo(LoanReviewOutbox.STATUS_FAILED);
        assertThat(outbox.getAttemptNo()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("kafka down");
        assertThat(outbox.getNextAttemptAt()).isAfter(now);
    }

    @Test
    void markFailed_상한_도달시_DEAD() {
        var outbox = LoanReviewOutbox.caseIndexed(1L, "{}");
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 0; i < LoanReviewOutbox.DEFAULT_MAX_ATTEMPT; i++) {
            outbox.markFailed("err", now);
        }

        assertThat(outbox.getStatus()).isEqualTo(LoanReviewOutbox.STATUS_DEAD);
        assertThat(outbox.getAttemptNo()).isEqualTo(LoanReviewOutbox.DEFAULT_MAX_ATTEMPT);
    }
}
