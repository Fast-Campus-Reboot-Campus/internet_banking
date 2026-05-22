package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 이메일 어댑터 stub. */
@Slf4j
@Component
public class StubEmailAdapter implements NotificationChannelAdapter {

    public static final String CHANNEL_CD = "EMAIL";

    @Override
    public String getChannelCd() {
        return CHANNEL_CD;
    }

    @Override
    public SendResult send(NotificationOutbox row) {
        String providerMsgId = "EML-" + UUID.randomUUID().toString().substring(0, 12);
        log.debug("[noti-email] outboxId={} ref={} sent (stub)", row.getOutboxId(), row.getReferenceId());
        return new SendResult(true, providerMsgId, "0000", "OK");
    }
}
