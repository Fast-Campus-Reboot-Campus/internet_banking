package com.bank.loan.advisory.listener;

import com.bank.loan.advisory.event.AdvisoryReportPublishedEvent;
import com.bank.loan.advisory.kafka.AdvisoryKafkaOutboxAppender;
import com.bank.loan.advisory.kafka.AdvisoryKafkaOutboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdvisoryReportPublishedListenerTest {

    AdvisoryKafkaOutboxAppender kafkaOutboxAppender = mock(AdvisoryKafkaOutboxAppender.class);
    AdvisoryReportPublishedListener listener;

    @BeforeEach
    void setUp() {
        listener = new AdvisoryReportPublishedListener(kafkaOutboxAppender);
    }

    @Test
    void 리포트_발행_이벤트_수신_시_Kafka_outbox_적재() {
        AdvisoryReportPublishedEvent event = new AdvisoryReportPublishedEvent(
                501L, 201L, "CRITICAL", "2026-05-27");

        listener.onReportPublished(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaOutboxAppender).enqueue(
                eq(AdvisoryReportPublishedListener.EVENT_TYPE),
                eq("501"),
                eq(AdvisoryKafkaOutboxMessage.TOPIC_REPORT),
                eq("501"),
                payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"advrId\":501");
        assertThat(payload).contains("\"targetReviewerId\":201");
        assertThat(payload).contains("\"severityCd\":\"CRITICAL\"");
        assertThat(payload).contains("\"baseDate\":\"2026-05-27\"");
    }

    @Test
    void targetReviewerId_null_이어도_예외_없이_적재() {
        AdvisoryReportPublishedEvent event = new AdvisoryReportPublishedEvent(
                502L, null, "WARN", "2026-05-27");

        assertThatNoException().isThrownBy(() -> listener.onReportPublished(event));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaOutboxAppender).enqueue(any(), any(), any(), any(), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"targetReviewerId\":null");
    }

    @Test
    void kafka_outbox_적재_실패해도_예외_전파_안함() {
        doThrow(new RuntimeException("DB 오류"))
                .when(kafkaOutboxAppender).enqueue(any(), any(), any(), any(), any());

        AdvisoryReportPublishedEvent event = new AdvisoryReportPublishedEvent(
                503L, 202L, "INFO", "2026-05-27");

        assertThatNoException().isThrownBy(() -> listener.onReportPublished(event));
    }

    @Test
    void INFO_WARN_CRITICAL_severity_모두_적재() {
        for (String severity : new String[]{"INFO", "WARN", "CRITICAL"}) {
            AdvisoryReportPublishedEvent event = new AdvisoryReportPublishedEvent(
                    504L, 203L, severity, "2026-05-27");
            listener.onReportPublished(event);
        }

        verify(kafkaOutboxAppender, times(3)).enqueue(any(), any(), any(), any(), any());
    }
}
