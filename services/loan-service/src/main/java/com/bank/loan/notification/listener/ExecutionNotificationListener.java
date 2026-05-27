package com.bank.loan.notification.listener;

import com.bank.loan.notification.channel.StubEmailAdapter;
import com.bank.loan.notification.channel.StubKakaoAdapter;
import com.bank.loan.notification.channel.StubSmsAdapter;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.LoanDisbursedEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 자금 인출 완료 → 알림 outbox 적재 (SMS / 알림톡 / 이메일).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionNotificationListener {

    public static final String EVENT_TYPE = "LOAN_DISBURSED";

    private final NotificationOutboxAppender outboxAppender;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanDisbursed(LoanDisbursedEvent event) {
        String payload = String.format(
                "{\"cntrId\":%d,\"cntrNo\":\"%s\",\"customerId\":%d,\"executedAmount\":%d}",
                event.cntrId(), event.cntrNo(), event.customerId(), event.executedAmount());
        outboxAppender.enqueue(EVENT_TYPE, event.cntrId(), StubSmsAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.cntrId(), StubKakaoAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.cntrId(), StubEmailAdapter.CHANNEL_CD, payload);
        log.debug("[noti-outbox] enqueued {} for cntrId={}", EVENT_TYPE, event.cntrId());
    }
}
