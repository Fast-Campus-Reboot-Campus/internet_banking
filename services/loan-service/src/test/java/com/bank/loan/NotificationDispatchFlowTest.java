package com.bank.loan;

import com.bank.loan.notification.outbox.NotificationOutbox;
import com.bank.loan.notification.outbox.NotificationOutboxRepository;
import com.bank.loan.notification.service.NotificationDispatchService;
import com.bank.loan.support.AbstractLoanIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * plan 03 step 4: notification 디스패치 성공 경로 통합 테스트. 연도 2035.
 *
 * 리스너의 @Async race 를 피하려고 outbox row 를 직접 적재한 뒤 dispatch 엔드포인트를 호출한다.
 * (listener → outbox 적재 흐름은 step 7 회귀 테스트에서 다룬다.)
 */
class NotificationDispatchFlowTest extends AbstractLoanIntegrationTest {

    @Autowired private NotificationOutboxRepository outboxRepository;
    @Autowired private NotificationDispatchService dispatchService;

    @Test
    void pending_row_dispatch_시_SENT_전이() throws Exception {
        long uniqueRef = 9_001_001L + (long) (Math.random() * 1_000_000);
        NotificationOutbox row = outboxRepository.save(NotificationOutbox.builder()
                .eventTypeCd("APPLICATION_SUBMITTED")
                .referenceId(uniqueRef)
                .channelCd("SMS")
                .payload("{\"test\":\"dispatch\"}")
                .status(NotificationOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(5)
                .nextAttemptAt(OffsetDateTime.now())
                .idempotencyKey(NotificationOutbox.idempotencyKeyOf("APPLICATION_SUBMITTED", uniqueRef, "SMS"))
                .build());

        mockMvc.perform(post("/api/internal/notifications/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").isNumber())
                .andExpect(jsonPath("$.data.sent").isNumber());

        NotificationOutbox after = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(row.getOutboxId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo("SENT");
        assertThat(after.getSentAt()).isNotNull();
        assertThat(after.getAttemptNo()).isZero();
    }

    @Test
    void 미래_nextAttemptAt_은_dispatch_가_픽업_안_함() {
        long uniqueRef = 9_002_001L + (long) (Math.random() * 1_000_000);
        NotificationOutbox row = outboxRepository.save(NotificationOutbox.builder()
                .eventTypeCd("LOAN_APPROVED")
                .referenceId(uniqueRef)
                .channelCd("EMAIL")
                .payload("{}")
                .status(NotificationOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(5)
                .nextAttemptAt(OffsetDateTime.now().plusHours(1))
                .idempotencyKey(NotificationOutbox.idempotencyKeyOf("LOAN_APPROVED", uniqueRef, "EMAIL"))
                .build());

        dispatchService.dispatch();

        NotificationOutbox after = outboxRepository
                .findByOutboxIdAndDeletedAtIsNull(row.getOutboxId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo("PENDING");
        assertThat(after.getAttemptNo()).isZero();
    }
}
