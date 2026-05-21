package com.bank.loan.notification.listener;

import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.ApplicationSubmittedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 신청 접수 → SMS/알림톡/이메일 접수 안내 (fire-and-forget).
 * 모델은 ContractNotificationListener 와 동일 (AFTER_COMMIT + @Async + stub IO).
 */
@Slf4j
@Component
public class ApplicationNotificationListener {

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        String thread = Thread.currentThread().getName();
        log.info("[noti] thread={} application submitted: applId={}, applNo={}, customerId={}",
                thread, event.applId(), event.applNo(), event.customerId());
        simulate("sms",   event.applId(), 30);
        simulate("kakao-alimtalk", event.applId(), 40);
        simulate("email", event.applId(), 60);
        log.info("[noti] thread={} applId={} application-submitted done", thread, event.applId());
    }

    private void simulate(String channel, Long id, long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[noti] channel={} applId={} sent ({}ms stub)", channel, id, ms);
    }
}
