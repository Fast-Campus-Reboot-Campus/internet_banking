package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** SMS 어댑터 stub. 실 게이트웨이 도입 전까지 무조건 성공. */
@Slf4j
@Component
public class StubSmsAdapter implements NotificationChannelAdapter {

    public static final String CHANNEL_CD = "SMS";

    @Override
    public String getChannelCd() {
        return CHANNEL_CD;
    }

    @Override
    public SendResult send(NotificationOutbox row) {
        String providerMsgId = "SMS-" + UUID.randomUUID().toString().substring(0, 12);
        log.debug("[noti-sms] outboxId={} ref={} sent (stub)", row.getOutboxId(), row.getReferenceId());
        return new SendResult(true, providerMsgId, "0000", "OK");
    }
}
