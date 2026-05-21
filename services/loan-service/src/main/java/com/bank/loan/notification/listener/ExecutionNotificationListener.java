package com.bank.loan.notification.listener;

import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.LoanDisbursedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 자금 인출 완료 → SMS/알림톡/이메일 실행 안내 + 상환 일정 발송 (fire-and-forget).
 */
@Slf4j
@Component
public class ExecutionNotificationListener {

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanDisbursed(LoanDisbursedEvent event) {
        String thread = Thread.currentThread().getName();
        log.info("[noti] thread={} loan disbursed: cntrId={}, cntrNo={}, executedAmount={}",
                thread, event.cntrId(), event.cntrNo(), event.executedAmount());
        simulate("sms",   event.cntrId(), 30);
        simulate("kakao-alimtalk", event.cntrId(), 40);
        simulate("email", event.cntrId(), 60);
        log.info("[noti] thread={} cntrId={} loan-disbursed done", thread, event.cntrId());
    }

    private void simulate(String channel, Long id, long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[noti] channel={} cntrId={} sent ({}ms stub)", channel, id, ms);
    }
}
