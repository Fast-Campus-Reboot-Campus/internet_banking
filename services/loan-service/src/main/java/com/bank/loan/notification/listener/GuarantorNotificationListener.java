package com.bank.loan.notification.listener;

import com.bank.loan.guarantor.domain.GuarantorAgreement;
import com.bank.loan.notification.channel.StubOperatorAlertAdapter;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.GuarantorCanceledEvent;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 보증 약정 취소 → 운영자 알람 outbox 적재.
 *
 * REGISTERED 취소: 보증인이 서명 전에 빠진 케이스 — 운영자 개입 불필요 (본심사 사전조건에서 이미 차단).
 * SIGNED 취소    : 활성 보증인 수가 minGuarantorCount 아래로 떨어질 수 있으므로 운영자에게 즉시 알림.
 *                  이후 약정(LOAN_175) 및 실행(LOAN_176) 진입 시 검증에서 차단되므로 별도 자동 롤백은 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuarantorNotificationListener {

    public static final String EVENT_TYPE = "GUARANTOR_CANCELED";

    private final NotificationOutboxAppender outboxAppender;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGuarantorCanceled(GuarantorCanceledEvent event) {
        // REGISTERED → CANCELED 는 보증인이 아직 유효하지 않았으므로 운영자 알람 불필요.
        if (!GuarantorAgreement.STATUS_SIGNED.equals(event.prevStatusCd())) {
            return;
        }
        String payload = String.format(
                "{\"applId\":%d,\"gagrId\":%d,\"prevStatusCd\":\"%s\"}",
                event.applId(), event.gagrId(), event.prevStatusCd());
        outboxAppender.enqueue(EVENT_TYPE, event.gagrId(), StubOperatorAlertAdapter.CHANNEL_CD, payload);
        log.debug("[noti-outbox] enqueued {} for gagrId={}", EVENT_TYPE, event.gagrId());
    }
}
