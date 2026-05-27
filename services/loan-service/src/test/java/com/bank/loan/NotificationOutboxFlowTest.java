package com.bank.loan;

import com.bank.loan.notification.channel.NotificationChannelAdapter;
import com.bank.loan.notification.outbox.NotificationOutbox;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import com.bank.loan.notification.outbox.NotificationOutboxRepository;
import com.bank.loan.notification.service.NotificationDispatchService;
import com.bank.loan.notification.service.NotificationOutboxQueryService;
import com.bank.loan.support.AbstractLoanIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * plan 03 step 6: notification outbox 라이프사이클. 연도 2035.
 *
 *   1) 어댑터 실패 → FAILED + attemptNo++ + exp backoff
 *   2) 누적 실패가 maxAttempt 도달 → DEAD
 *   3) idempotencyKey 가 중복 enqueue 를 차단
 *   4) 운영자 retry 가 FAILED 를 PENDING 으로 되돌림
 *   5) SENT 에 retry → LOAN_191
 *   6) 운영자 목록 조회의 status 필터 동작
 */
@Import(NotificationOutboxFlowTest.FailingAdapterConfig.class)
class NotificationOutboxFlowTest extends AbstractLoanIntegrationTest {

    @TestConfiguration
    static class FailingAdapterConfig {
        public static final String CHANNEL_FAIL = "TEST_FAIL";

        @Bean
        NotificationChannelAdapter failingNotificationAdapter() {
            return new NotificationChannelAdapter() {
                @Override public String getChannelCd() { return CHANNEL_FAIL; }
                @Override public SendResult send(NotificationOutbox row) {
                    return new SendResult(false, null, "9999", "stub failure");
                }
            };
        }
    }

    @Autowired private NotificationOutboxRepository outboxRepository;
    @Autowired private NotificationOutboxAppender outboxAppender;
    @Autowired private NotificationDispatchService dispatchService;
    @Autowired private NotificationOutboxQueryService queryService;

    /** 다른 테스트 클래스와 referenceId 가 겹치지 않게 격리한다. */
    private static final AtomicLong REF_SEQ = new AtomicLong(9_500_000L);
    private static long nextRef() { return REF_SEQ.incrementAndGet(); }

    @Test
    void 어댑터_실패_시_FAILED_와_attempt_증가_그리고_backoff() {
        long ref = nextRef();
        NotificationOutbox saved = enqueuePending("OUTBOX_LIFECYCLE_A", ref, FailingAdapterConfig.CHANNEL_FAIL);

        OffsetDateTime beforeDispatch = OffsetDateTime.now();
        dispatchService.dispatch();

        NotificationOutbox after = reload(saved.getOutboxId());
        assertThat(after.getStatus()).isEqualTo(NotificationOutbox.STATUS_FAILED);
        assertThat(after.getAttemptNo()).isEqualTo(1);
        assertThat(after.getLastError()).contains("stub failure");
        // backoff: nextAttemptAt = now + 2^1 분.
        assertThat(after.getNextAttemptAt()).isAfter(beforeDispatch.plusSeconds(60));
    }

    @Test
    void 최대_시도_초과하면_DEAD_로_전이() {
        long ref = nextRef();
        NotificationOutbox saved = enqueuePending("OUTBOX_LIFECYCLE_B", ref, FailingAdapterConfig.CHANNEL_FAIL);

        // maxAttempt(5) 도달까지 dispatch 반복. 매 사이클마다 nextAttemptAt 을 now 로 끌어와야 다음 픽업이 됨.
        for (int i = 0; i < NotificationOutbox.DEFAULT_MAX_ATTEMPT; i++) {
            NotificationOutbox row = reload(saved.getOutboxId());
            if (NotificationOutbox.STATUS_DEAD.equals(row.getStatus())) break;
            row.delayNextAttempt(OffsetDateTime.now());
            outboxRepository.save(row);
            dispatchService.dispatch();
        }

        NotificationOutbox dead = reload(saved.getOutboxId());
        assertThat(dead.getStatus()).isEqualTo(NotificationOutbox.STATUS_DEAD);
        assertThat(dead.getAttemptNo()).isGreaterThanOrEqualTo(dead.getMaxAttempt());
    }

    @Test
    void 같은_이벤트_두_번_enqueue_는_idempotencyKey_로_차단() {
        String eventType = "OUTBOX_LIFECYCLE_C";
        long ref = nextRef();

        outboxAppender.enqueue(eventType, ref, "SMS", "{\"tag\":\"first\"}");
        outboxAppender.enqueue(eventType, ref, "SMS", "{\"tag\":\"second\"}");

        String key = NotificationOutbox.idempotencyKeyOf(eventType, ref, "SMS");
        NotificationOutbox row = outboxRepository.findByIdempotencyKeyAndDeletedAtIsNull(key).orElseThrow();
        // 두 번째 enqueue 는 무시되므로 첫 번째 payload 가 유지된다.
        assertThat(row.getPayload()).contains("first").doesNotContain("second");
    }

    @Test
    void 운영자_retry_는_FAILED_를_PENDING_으로_되돌린다() throws Exception {
        long ref = nextRef();
        NotificationOutbox saved = enqueuePending("OUTBOX_LIFECYCLE_D", ref, FailingAdapterConfig.CHANNEL_FAIL);
        dispatchService.dispatch();
        assertThat(reload(saved.getOutboxId()).getStatus()).isEqualTo(NotificationOutbox.STATUS_FAILED);

        mockMvc.perform(post("/api/notifications/{id}/retry", saved.getOutboxId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(NotificationOutbox.STATUS_PENDING))
                .andExpect(jsonPath("$.data.attemptNo").value(0));

        NotificationOutbox after = reload(saved.getOutboxId());
        assertThat(after.getStatus()).isEqualTo(NotificationOutbox.STATUS_PENDING);
        assertThat(after.getAttemptNo()).isZero();
        assertThat(after.getLastError()).isNull();
    }

    @Test
    void SENT_상태에서_retry_는_LOAN_191() throws Exception {
        long ref = nextRef();
        // 성공 채널(SMS) 로 dispatch → SENT 전이.
        NotificationOutbox saved = enqueuePending("OUTBOX_LIFECYCLE_E", ref, "SMS");
        dispatchService.dispatch();
        assertThat(reload(saved.getOutboxId()).getStatus()).isEqualTo(NotificationOutbox.STATUS_SENT);

        mockMvc.perform(post("/api/notifications/{id}/retry", saved.getOutboxId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_191"));

        assertThatThrownBy(() -> queryService.retry(saved.getOutboxId()))
                .isInstanceOf(com.bank.common.web.BusinessException.class)
                .hasMessageContaining("current=SENT");
    }

    @Test
    void 목록_조회는_status_필터를_적용한다() throws Exception {
        long ref = nextRef();
        String eventType = "OUTBOX_LIFECYCLE_F";
        NotificationOutbox row = enqueuePending(eventType, ref, FailingAdapterConfig.CHANNEL_FAIL);
        dispatchService.dispatch();
        assertThat(reload(row.getOutboxId()).getStatus()).isEqualTo(NotificationOutbox.STATUS_FAILED);

        mockMvc.perform(get("/api/notifications")
                        .param("eventType", eventType)
                        .param("status", NotificationOutbox.STATUS_FAILED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].outboxId").value(row.getOutboxId()))
                .andExpect(jsonPath("$.data.items[0].status").value(NotificationOutbox.STATUS_FAILED));

        mockMvc.perform(get("/api/notifications")
                        .param("eventType", eventType)
                        .param("status", NotificationOutbox.STATUS_SENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    private NotificationOutbox enqueuePending(String eventTypeCd, long ref, String channelCd) {
        return outboxRepository.save(NotificationOutbox.builder()
                .eventTypeCd(eventTypeCd)
                .referenceId(ref)
                .channelCd(channelCd)
                .payload("{\"ref\":" + ref + "}")
                .status(NotificationOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(NotificationOutbox.DEFAULT_MAX_ATTEMPT)
                .nextAttemptAt(OffsetDateTime.now())
                .idempotencyKey(NotificationOutbox.idempotencyKeyOf(eventTypeCd, ref, channelCd))
                .build());
    }

    private NotificationOutbox reload(Long outboxId) {
        return outboxRepository.findByOutboxIdAndDeletedAtIsNull(outboxId).orElseThrow();
    }
}
