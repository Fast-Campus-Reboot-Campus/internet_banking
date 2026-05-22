package com.bank.loan.notification.listener;

import com.bank.loan.notification.channel.StubEmailAdapter;
import com.bank.loan.notification.channel.StubKakaoAdapter;
import com.bank.loan.notification.channel.StubSmsAdapter;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.ContractSignedEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 약정 체결 → 알림 outbox 적재 (SMS / 알림톡 / 이메일).
 * 약정서 PDF 생성은 document 도메인 책임 — 본 listener 의 관심 밖으로 분리됐다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractNotificationListener {

    public static final String EVENT_TYPE = "CONTRACT_SIGNED";

    private final NotificationOutboxAppender outboxAppender;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractSigned(ContractSignedEvent event) {
        String payload = String.format(
                "{\"cntrId\":%d,\"cntrNo\":\"%s\",\"applId\":%d,\"customerId\":%d}",
                event.cntrId(), event.cntrNo(), event.applId(), event.customerId());
        outboxAppender.enqueue(EVENT_TYPE, event.cntrId(), StubSmsAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.cntrId(), StubKakaoAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.cntrId(), StubEmailAdapter.CHANNEL_CD, payload);
        log.debug("[noti-outbox] enqueued {} for cntrId={}", EVENT_TYPE, event.cntrId());
    }
}
