package com.bank.loan.virtualaccount;

import com.bank.loan.virtualaccount.kafka.VirtualAccountDepositConsumer;
import com.bank.loan.virtualaccount.service.VirtualAccountDepositService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VirtualAccountDepositConsumerTest {

    @Mock VirtualAccountDepositService depositService;
    @Mock Acknowledgment ack;

    private VirtualAccountDepositConsumer newConsumer() {
        return new VirtualAccountDepositConsumer(new ObjectMapper(), depositService);
    }

    private ConsumerRecord<String, String> record(String json) {
        return new ConsumerRecord<>("payment.completed", 0, 0L, "PI-1", json);
    }

    @Test
    void 인바운드입금완결_handleInboundDeposit_호출_및_ack() {
        String json = "{\"paymentInstructionId\":\"PI-1\",\"direction\":\"IN\","
                + "\"status\":\"COMPLETED\",\"transferAmount\":500000}";

        newConsumer().consume(record(json), ack);

        verify(depositService).handleInboundDeposit(any());
        verify(ack).acknowledge();
    }

    @Test
    void 아웃바운드_OUT_무시_handle_미호출() {
        String json = "{\"paymentInstructionId\":\"PI-2\",\"direction\":\"OUT\","
                + "\"status\":\"COMPLETED\",\"transferAmount\":100000}";

        newConsumer().consume(record(json), ack);

        verify(depositService, never()).handleInboundDeposit(any());
        verify(ack).acknowledge();
    }

    @Test
    void 인바운드지만_status_PENDING_무시() {
        String json = "{\"paymentInstructionId\":\"PI-3\",\"direction\":\"IN\","
                + "\"status\":\"PENDING\",\"transferAmount\":200000}";

        newConsumer().consume(record(json), ack);

        verify(depositService, never()).handleInboundDeposit(any());
        verify(ack).acknowledge();
    }

    @Test
    void direction_필드_없음_무시() {
        String json = "{\"paymentInstructionId\":\"PI-4\",\"status\":\"COMPLETED\",\"transferAmount\":300000}";

        newConsumer().consume(record(json), ack);

        verify(depositService, never()).handleInboundDeposit(any());
        verify(ack).acknowledge();
    }

    @Test
    void 잘못된_JSON_예외발생_그래도_ack() {
        ConsumerRecord<String, String> bad = new ConsumerRecord<>("payment.completed", 0, 0L, "X", "not-json");

        newConsumer().consume(bad, ack);

        verify(depositService, never()).handleInboundDeposit(any());
        verify(ack).acknowledge();
    }
}
