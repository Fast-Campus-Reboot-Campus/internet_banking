package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 앱 푸시 어댑터 stub (FCM/APNs 자리). */
@Slf4j
@Component
public class StubAppPushAdapter implements NotificationChannelAdapter {

    public static final String CHANNEL_CD = "APP_PUSH";

    @Override
    public String getChannelCd() {
        return CHANNEL_CD;
    }

    @Override
    public SendResult send(NotificationOutbox row) {
        String providerMsgId = "PSH-" + UUID.randomUUID().toString().substring(0, 12);
        log.debug("[noti-push] outboxId={} ref={} sent (stub)", row.getOutboxId(), row.getReferenceId());
        return new SendResult(true, providerMsgId, "0000", "OK");
    }
}
