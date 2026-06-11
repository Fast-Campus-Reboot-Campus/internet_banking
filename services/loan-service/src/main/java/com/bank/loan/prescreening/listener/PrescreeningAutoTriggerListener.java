package com.bank.loan.prescreening.listener;

import com.bank.loan.notification.event.ApplicationSubmittedEvent;
import com.bank.loan.prescreening.dto.RunPrescreeningRequest;
import com.bank.loan.prescreening.service.LoanPrescreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 신청 접수(SUBMITTED) 후 가심사를 자동 실행한다.
 *
 * - 신청 트랜잭션이 commit 된 뒤(AFTER_COMMIT) 비동기로 가심사 엔진을 호출한다.
 *   commit 전 실행 시 신청 row 가 아직 영속화되지 않아 LOAN_012 가 날 수 있으므로 AFTER_COMMIT 고정.
 * - prescResultCd 를 비워(null) 호출하므로 {@link com.bank.loan.prescreening.engine.CreditScoreEngine}
 *   가 PASS/REJECT 를 자동 판정한다. PASS 시 PrescreeningPassedEvent 가 발행되어
 *   신용평가→DSR 자동 트리거가 연쇄 실행된다.
 * - 이미 가심사된 건(LOAN_046)·가심사 불가 상태(LOAN_047) 등은 무시(로그만) — 멱등성 확보.
 * - 실패해도 신청 접수에는 영향 없음.
 * - {@code loan.auto-trigger.enabled=false} 로 비활성화 가능(통합테스트는 가심사를 직접 통제).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "loan.auto-trigger.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PrescreeningAutoTriggerListener {

    private static final RunPrescreeningRequest ENGINE_DECIDES =
            new RunPrescreeningRequest(null, null, null, null, null, null, null, null);

    private final LoanPrescreeningService prescreeningService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        try {
            prescreeningService.run(event.applId(), ENGINE_DECIDES);
            log.info("가심사 자동 실행 완료: applId={}", event.applId());
        } catch (Exception e) {
            log.warn("가심사 자동 실행 실패 (무시) applId={}: {}", event.applId(), e.getMessage());
        }
    }
}
