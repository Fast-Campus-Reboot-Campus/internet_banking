package com.bank.loan.notification.listener;

import com.bank.loan.notification.channel.StubEmailAdapter;
import com.bank.loan.notification.channel.StubKakaoAdapter;
import com.bank.loan.notification.channel.StubSmsAdapter;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.ApplicationSubmittedEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 신청 접수 → 알림 outbox 적재 (SMS / 알림톡 / 이메일).
 * 외부 송신은 dispatch 배치(별 컴포넌트) 책임 — AI_GUIDELINES: 트랜잭션 내 외부 API 금지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationNotificationListener {

    public static final String EVENT_TYPE = "APPLICATION_SUBMITTED";

    private final NotificationOutboxAppender outboxAppender;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        String payload = String.format(
                "{\"applId\":%d,\"applNo\":\"%s\",\"customerId\":%d,\"prodId\":%d}",
                event.applId(), event.applNo(), event.customerId(), event.prodId());
        outboxAppender.enqueue(EVENT_TYPE, event.applId(), StubSmsAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.applId(), StubKakaoAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.applId(), StubEmailAdapter.CHANNEL_CD, payload);
        log.debug("[noti-outbox] enqueued {} for applId={}", EVENT_TYPE, event.applId());
    }
}
