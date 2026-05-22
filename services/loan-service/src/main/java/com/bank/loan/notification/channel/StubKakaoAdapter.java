package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 카카오 알림톡 어댑터 stub. */
@Slf4j
@Component
public class StubKakaoAdapter implements NotificationChannelAdapter {

    public static final String CHANNEL_CD = "KAKAO_ALIMTALK";

    @Override
    public String getChannelCd() {
        return CHANNEL_CD;
    }

    @Override
    public SendResult send(NotificationOutbox row) {
        String providerMsgId = "KKO-" + UUID.randomUUID().toString().substring(0, 12);
        log.debug("[noti-kakao] outboxId={} ref={} sent (stub)", row.getOutboxId(), row.getReferenceId());
        return new SendResult(true, providerMsgId, "0000", "OK");
    }
}
