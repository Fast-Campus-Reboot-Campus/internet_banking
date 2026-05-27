package com.bank.loan.notification.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * plan 03 step 3: NotificationOutboxAppender 단위 검증 (Mockito).
 *
 *   1) 신규 호출 → save 호출 + idempotencyKey 정확히 셋업
 *   2) 동일 idempotencyKey 가 이미 있으면 save 호출 안 됨
 *   3) save 도중 DataIntegrityViolationException 이 나도 호출자에게 예외 전파 안 됨
 */
class NotificationOutboxAppenderTest {

    private NotificationOutboxRepository repository;
    private NotificationOutboxAppender appender;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationOutboxRepository.class);
        appender = new NotificationOutboxAppender(repository);
    }

    @Test
    void 신규_enqueue_는_save_호출() {
        when(repository.findByIdempotencyKeyAndDeletedAtIsNull(any()))
                .thenReturn(Optional.empty());

        appender.enqueue("APPLICATION_SUBMITTED", 100L, "SMS", "{\"foo\":1}");

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());
        NotificationOutbox saved = captor.getValue();
        assertThat(saved.getEventTypeCd()).isEqualTo("APPLICATION_SUBMITTED");
        assertThat(saved.getReferenceId()).isEqualTo(100L);
        assertThat(saved.getChannelCd()).isEqualTo("SMS");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getMaxAttempt()).isEqualTo(NotificationOutbox.DEFAULT_MAX_ATTEMPT);
        assertThat(saved.getIdempotencyKey()).isEqualTo("APPLICATION_SUBMITTED:100:SMS");
        assertThat(saved.getPayload()).isEqualTo("{\"foo\":1}");
    }

    @Test
    void 같은_idempotencyKey_가_이미_있으면_save_안_함() {
        NotificationOutbox existing = NotificationOutbox.builder()
                .eventTypeCd("APPLICATION_SUBMITTED").referenceId(100L).channelCd("SMS")
                .status("PENDING").attemptNo(0).maxAttempt(5)
                .nextAttemptAt(java.time.OffsetDateTime.now())
                .idempotencyKey("APPLICATION_SUBMITTED:100:SMS").build();
        when(repository.findByIdempotencyKeyAndDeletedAtIsNull(eq("APPLICATION_SUBMITTED:100:SMS")))
                .thenReturn(Optional.of(existing));

        appender.enqueue("APPLICATION_SUBMITTED", 100L, "SMS", "{}");

        verify(repository, never()).save(any());
    }

    @Test
    void save_race_DataIntegrityViolation_은_삼킨다() {
        when(repository.findByIdempotencyKeyAndDeletedAtIsNull(any()))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("unique violation"));

        // 호출자(listener)에게 예외가 전파되지 않아야 한다.
        appender.enqueue("LOAN_APPROVED", 200L, "EMAIL", "{}");
    }
}
