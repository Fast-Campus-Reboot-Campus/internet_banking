package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 운영자 알람 어댑터 stub. 실 채널(PagerDuty / Slack / 내부 알람 시스템) 도입 전까지 무조건 성공. */
@Slf4j
@Component
public class StubOperatorAlertAdapter implements NotificationChannelAdapter {

    public static final String CHANNEL_CD = "OP_ALERT";

    @Override
    public String getChannelCd() {
        return CHANNEL_CD;
    }

    @Override
    public SendResult send(NotificationOutbox row) {
        String providerMsgId = "OPA-" + UUID.randomUUID().toString().substring(0, 12);
        log.debug("[noti-op-alert] outboxId={} ref={} sent (stub)", row.getOutboxId(), row.getReferenceId());
        return new SendResult(true, providerMsgId, "0000", "OK");
    }
}
