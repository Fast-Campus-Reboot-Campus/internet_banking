package com.bank.loan.notification.listener;

import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.ContractSignedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 약정 체결 이벤트 → PDF 생성 + 다채널 안내 발송 (fire-and-forget).
 *
 * 실행 모델:
 *   - {@code @TransactionalEventListener(phase = AFTER_COMMIT)} — 도메인 트랜잭션이 commit 된 후에만 트리거
 *   - {@code @Async("notificationExecutor")} — 도메인 호출자 스레드를 막지 않는 별도 풀에서 실행
 *
 * 실패 정책: 외부 IO 실패는 도메인 트랜잭션과 무관 (이미 commit 됨). 본 단계는 로그만.
 * 운영에서는 재시도 큐(예: Redis Streams) 또는 알림 outbox 테이블 도입 권장 — 본 단계 외.
 *
 * PDF 생성·SMS·알림톡·이메일 송신은 stub — 실제 라이브러리(PDFBox/카카오 알림톡 SDK 등) 의존을
 * 추가하지 않고 로그 + 짧은 sleep 으로 비동기 동작을 시뮬레이션한다.
 */
@Slf4j
@Component
public class ContractNotificationListener {

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractSigned(ContractSignedEvent event) {
        String thread = Thread.currentThread().getName();
        log.info("[noti] thread={} contract signed: cntrId={}, cntrNo={}, applId={}, customerId={}",
                thread, event.cntrId(), event.cntrNo(), event.applId(), event.customerId());

        renderContractPdf(event);
        sendSms(event);
        sendKakao(event);
        sendEmail(event);

        log.info("[noti] thread={} cntrId={} done", thread, event.cntrId());
    }

    private void renderContractPdf(ContractSignedEvent event) {
        simulate("pdf-render", event.cntrId(), 80);
    }

    private void sendSms(ContractSignedEvent event) {
        simulate("sms", event.cntrId(), 30);
    }

    private void sendKakao(ContractSignedEvent event) {
        simulate("kakao-alimtalk", event.cntrId(), 40);
    }

    private void sendEmail(ContractSignedEvent event) {
        simulate("email", event.cntrId(), 60);
    }

    private void simulate(String channel, Long cntrId, long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[noti] channel={} cntrId={} sent ({}ms stub)", channel, cntrId, ms);
    }
}
