package com.bank.loan.notification.listener;

import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.LoanApprovedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 본심사 승인 → SMS/알림톡/이메일 승인 안내 (fire-and-forget).
 * D+14 약정 유효기간 안내 등은 운영 문구에서 처리 — 본 단계는 채널 송신 stub만.
 */
@Slf4j
@Component
public class ReviewNotificationListener {

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanApproved(LoanApprovedEvent event) {
        String thread = Thread.currentThread().getName();
        log.info("[noti] thread={} loan approved: applId={}, revId={}, approvedAmount={}",
                thread, event.applId(), event.revId(), event.approvedAmount());
        simulate("sms",   event.applId(), 30);
        simulate("kakao-alimtalk", event.applId(), 40);
        simulate("email", event.applId(), 60);
        log.info("[noti] thread={} applId={} loan-approved done", thread, event.applId());
    }

    private void simulate(String channel, Long id, long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[noti] channel={} applId={} sent ({}ms stub)", channel, id, ms);
    }
}
