package com.bank.loan.notification.listener;

import com.bank.loan.notification.channel.StubEmailAdapter;
import com.bank.loan.notification.channel.StubKakaoAdapter;
import com.bank.loan.notification.channel.StubSmsAdapter;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.LoanApprovedEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 본심사 승인 → 알림 outbox 적재 (SMS / 알림톡 / 이메일).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewNotificationListener {

    public static final String EVENT_TYPE = "LOAN_APPROVED";

    private final NotificationOutboxAppender outboxAppender;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanApproved(LoanApprovedEvent event) {
        String payload = String.format(
                "{\"applId\":%d,\"revId\":%d,\"customerId\":%d,\"approvedAmount\":%d}",
                event.applId(), event.revId(), event.customerId(), event.approvedAmount());
        outboxAppender.enqueue(EVENT_TYPE, event.applId(), StubSmsAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.applId(), StubKakaoAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.applId(), StubEmailAdapter.CHANNEL_CD, payload);
        log.debug("[noti-outbox] enqueued {} for applId={}", EVENT_TYPE, event.applId());
    }
}
