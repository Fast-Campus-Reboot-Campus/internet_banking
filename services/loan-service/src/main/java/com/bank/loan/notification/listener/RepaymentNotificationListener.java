package com.bank.loan.notification.listener;

import com.bank.loan.notification.channel.StubKakaoAdapter;
import com.bank.loan.notification.channel.StubSmsAdapter;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.InstallmentPaidEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회차 상환 완료 → 알림 outbox 적재 (SMS / 알림톡).
 * 이메일은 회차 단위에서는 과다 — 기본 채널 매트릭스에서 제외 (운영자 설정 가능 부분은 별 plan).
 *
 * referenceId 는 rtxId (상환 트랜잭션 PK) — 회차마다 별개 알림 row 가 적재된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentNotificationListener {

    public static final String EVENT_TYPE = "INSTALLMENT_PAID";

    private final NotificationOutboxAppender outboxAppender;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstallmentPaid(InstallmentPaidEvent event) {
        String payload = String.format(
                "{\"rtxId\":%d,\"cntrId\":%d,\"installmentNo\":%d,\"paidAmount\":%d,\"channelCd\":\"%s\"}",
                event.rtxId(), event.cntrId(), event.installmentNo(),
                event.paidAmount(), event.channelCd());
        outboxAppender.enqueue(EVENT_TYPE, event.rtxId(), StubSmsAdapter.CHANNEL_CD, payload);
        outboxAppender.enqueue(EVENT_TYPE, event.rtxId(), StubKakaoAdapter.CHANNEL_CD, payload);
        log.debug("[noti-outbox] enqueued {} for rtxId={}", EVENT_TYPE, event.rtxId());
    }
}
