package com.bank.loan.notification.listener;

import com.bank.loan.notification.event.ApplicationSubmittedEvent;
import com.bank.loan.notification.event.ContractSignedEvent;
import com.bank.loan.notification.event.InstallmentPaidEvent;
import com.bank.loan.notification.event.LoanApprovedEvent;
import com.bank.loan.notification.event.LoanDisbursedEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * plan 03 step 3: 각 listener 가 채널 매트릭스대로 outbox 를 적재하는지 단위 검증 (Mockito).
 */
class NotificationListenerOutboxTest {

    @Test
    void application_은_SMS_KAKAO_EMAIL_3종_적재() {
        NotificationOutboxAppender appender = mock(NotificationOutboxAppender.class);
        new ApplicationNotificationListener(appender)
                .onApplicationSubmitted(new ApplicationSubmittedEvent(100L, "APP-001", 5L, 9L));

        ArgumentCaptor<String> evt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ch = ArgumentCaptor.forClass(String.class);
        verify(appender, times(3)).enqueue(evt.capture(), anyLong(), ch.capture(), anyString());
        assertThat(evt.getAllValues()).containsOnly("APPLICATION_SUBMITTED");
        assertThat(ch.getAllValues()).containsExactlyInAnyOrder("SMS", "KAKAO_ALIMTALK", "EMAIL");
    }

    @Test
    void contract_은_SMS_KAKAO_EMAIL_3종_적재() {
        NotificationOutboxAppender appender = mock(NotificationOutboxAppender.class);
        new ContractNotificationListener(appender)
                .onContractSigned(new ContractSignedEvent(200L, "CN-001", 100L, 5L));

        ArgumentCaptor<String> ch = ArgumentCaptor.forClass(String.class);
        verify(appender, times(3)).enqueue(anyString(), anyLong(), ch.capture(), anyString());
        assertThat(ch.getAllValues()).containsExactlyInAnyOrder("SMS", "KAKAO_ALIMTALK", "EMAIL");
    }

    @Test
    void execution_은_SMS_KAKAO_EMAIL_3종_적재() {
        NotificationOutboxAppender appender = mock(NotificationOutboxAppender.class);
        new ExecutionNotificationListener(appender)
                .onLoanDisbursed(new LoanDisbursedEvent(200L, "CN-001", 5L, 1_000_000L));

        ArgumentCaptor<String> ch = ArgumentCaptor.forClass(String.class);
        verify(appender, times(3)).enqueue(anyString(), anyLong(), ch.capture(), anyString());
        assertThat(ch.getAllValues()).containsExactlyInAnyOrder("SMS", "KAKAO_ALIMTALK", "EMAIL");
    }

    @Test
    void repayment_은_SMS_KAKAO_2종_적재_이메일_없음() {
        NotificationOutboxAppender appender = mock(NotificationOutboxAppender.class);
        new RepaymentNotificationListener(appender)
                .onInstallmentPaid(new InstallmentPaidEvent(300L, 200L, 11L, 1, 100_000L, "MANUAL"));

        ArgumentCaptor<String> ch = ArgumentCaptor.forClass(String.class);
        verify(appender, times(2)).enqueue(anyString(), anyLong(), ch.capture(), anyString());
        assertThat(ch.getAllValues()).containsExactlyInAnyOrder("SMS", "KAKAO_ALIMTALK");
        assertThat(List.copyOf(ch.getAllValues())).doesNotContain("EMAIL");
    }

    @Test
    void review_는_SMS_KAKAO_EMAIL_3종_적재() {
        NotificationOutboxAppender appender = mock(NotificationOutboxAppender.class);
        new ReviewNotificationListener(appender)
                .onLoanApproved(new LoanApprovedEvent(100L, 500L, 5L, 10_000_000L));

        ArgumentCaptor<String> ch = ArgumentCaptor.forClass(String.class);
        verify(appender, times(3)).enqueue(anyString(), anyLong(), ch.capture(), anyString());
        assertThat(ch.getAllValues()).containsExactlyInAnyOrder("SMS", "KAKAO_ALIMTALK", "EMAIL");
    }
}
