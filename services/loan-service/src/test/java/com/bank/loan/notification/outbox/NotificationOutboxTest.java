package com.bank.loan.notification.outbox;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plan 03 step 1: notification outbox 상태 머신 단위 검증.
 */
class NotificationOutboxTest {

    private NotificationOutbox newPending() {
        return NotificationOutbox.builder()
                .eventTypeCd("APPLICATION_SUBMITTED")
                .referenceId(11L)
                .channelCd("SMS")
                .payload("{}")
                .status(NotificationOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(3)
                .nextAttemptAt(OffsetDateTime.now())
                .idempotencyKey("APPLICATION_SUBMITTED:11:SMS")
                .build();
    }

    @Test
    void idempotencyKey_조합_규칙() {
        assertThat(NotificationOutbox.idempotencyKeyOf("LOAN_APPROVED", 42L, "KAKAO_ALIMTALK"))
                .isEqualTo("LOAN_APPROVED:42:KAKAO_ALIMTALK");
    }

    @Test
    void markSent_은_SENT_와_sentAt_을_채움() {
        NotificationOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.now();
        o.markSent(now);
        assertThat(o.getStatus()).isEqualTo("SENT");
        assertThat(o.getSentAt()).isEqualTo(now);
    }

    @Test
    void markFailed_1회_2분_백오프_FAILED() {
        NotificationOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.parse("2035-01-01T00:00:00Z");
        o.markFailed("provider 502", now);

        assertThat(o.getStatus()).isEqualTo("FAILED");
        assertThat(o.getAttemptNo()).isEqualTo(1);
        assertThat(o.getLastError()).isEqualTo("provider 502");
        assertThat(ChronoUnit.MINUTES.between(now, o.getNextAttemptAt())).isEqualTo(2);
    }

    @Test
    void maxAttempt_도달은_DEAD() {
        NotificationOutbox o = newPending();
        OffsetDateTime now = OffsetDateTime.now();
        o.markFailed("e", now);
        o.markFailed("e", now);
        o.markFailed("e", now);

        assertThat(o.getStatus()).isEqualTo("DEAD");
        assertThat(o.getAttemptNo()).isEqualTo(3);
    }

    @Test
    void requeue_는_PENDING_으로_초기화() {
        NotificationOutbox o = newPending();
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
}
