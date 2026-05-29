package com.bank.loan.autodebit;

import com.bank.loan.autodebit.kafka.PaymentEventConsumer;
import com.bank.loan.autodebit.service.ClearingResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock ClearingResultService clearingResultService;
    @Mock Acknowledgment ack;

    private PaymentEventConsumer newConsumer() {
        return new PaymentEventConsumer(new ObjectMapper(), clearingResultService);
    }

    private ConsumerRecord<String, String> record(String topic, String json) {
        return new ConsumerRecord<>(topic, 0, 0L, "PI-1", json);
    }

    @Test
    void completed_이벤트_handle_true_호출_및_ack() {
        String json = "{\"paymentInstructionId\":\"PI-1\",\"status\":\"COMPLETED\",\"amount\":1000}";

        newConsumer().consume(record("payment.completed", json), ack);

        verify(clearingResultService).handle("PI-1", true);
        verify(ack).acknowledge();
    }

    @Test
    void failed_이벤트_handle_false_호출_및_ack() {
        String json = "{\"paymentInstructionId\":\"PI-1\",\"status\":\"FAILED\",\"failureCategory\":\"KFTC_REJECTED\"}";

        newConsumer().consume(record("payment.failed", json), ack);

        verify(clearingResultService).handle("PI-1", false);
        verify(ack).acknowledge();
    }

    @Test
    void status없는_KFTC_SETTLED_무시_handle_미호출() {
        // KFTC_SETTLED/BOK_CONFIRMED 는 payment.completed 에 오지만 status 필드가 없음 → 회계계용, 무시
        String json = "{\"paymentInstructionId\":\"PI-1\",\"clearingNo\":\"CLR-1\",\"receiverBankCode\":\"088\"}";

        newConsumer().consume(record("payment.completed", json), ack);

        verify(clearingResultService, never()).handle(anyString(), anyBoolean());
        verify(ack).acknowledge();
    }

    @Test
    void piId없는_이벤트_무시_handle_미호출() {
        String json = "{\"paymentInstructionNo\":\"PI-1\",\"status\":\"FAILED\"}";

        newConsumer().consume(record("payment.reversed", json), ack);

        verify(clearingResultService, never()).handle(anyString(), anyBoolean());
        verify(ack).acknowledge();
    }

    @Test
    void 알수없는_status_무시_handle_미호출() {
        String json = "{\"paymentInstructionId\":\"PI-1\",\"status\":\"PROCESSING\"}";

        newConsumer().consume(record("payment.completed", json), ack);

        verify(clearingResultService, never()).handle(anyString(), eq(true));
        verify(ack, times(1)).acknowledge();
    }
}
