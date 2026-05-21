package com.bank.loan.notification.listener;

import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.InstallmentPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회차 상환 완료 → SMS/알림톡 출금/상환 알림 (fire-and-forget).
 * 자동이체(AUTO_DEBIT) 와 수동(MANUAL) 채널은 문구가 다르지만 본 단계 stub 은 동일 처리.
 */
@Slf4j
@Component
public class RepaymentNotificationListener {

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstallmentPaid(InstallmentPaidEvent event) {
        String thread = Thread.currentThread().getName();
        log.info("[noti] thread={} installment paid: rtxId={}, cntrId={}, installmentNo={}, amount={}, channel={}",
                thread, event.rtxId(), event.cntrId(), event.installmentNo(),
                event.paidAmount(), event.channelCd());
        simulate("sms",   event.cntrId(), 30);
        simulate("kakao-alimtalk", event.cntrId(), 40);
        log.info("[noti] thread={} cntrId={} installment-paid done", thread, event.cntrId());
    }

    private void simulate(String channel, Long id, long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[noti] channel={} cntrId={} sent ({}ms stub)", channel, id, ms);
    }
}
