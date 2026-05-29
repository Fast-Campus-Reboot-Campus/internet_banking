package com.bank.payment.scheduler;

import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.domain.mapper.PaymentInstructionMapper;
import com.bank.payment.domain.service.PaymentOrchestrator;
import com.bank.payment.domain.service.PaymentTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 예약이체 실행 폴링워커.
 *
 * scheduled_execution_at < now AND status='SCHEDULED' 인 PI = 실행 시각 도래한 예약이체.
 * 단계 2: claim(SCHEDULED→PROCESSING 선점).
 * 단계 3: executeScheduledIntraBank(claim 성공 pi) — 자행 완결. 실패/보상은 3-C에서 추가.
 * ★ @Transactional 없음 — claim(독립 TX)과 execute(별도 TX) 분리.
 */
@Slf4j
@Component
public class ScheduledPaymentWorker {

    private final PaymentInstructionMapper paymentInstructionMapper;
    private final PaymentTransactionService txService;
    private final PaymentOrchestrator orchestrator;

    public ScheduledPaymentWorker(PaymentInstructionMapper paymentInstructionMapper,
                                  PaymentTransactionService txService,
                                  PaymentOrchestrator orchestrator) {
        this.paymentInstructionMapper = paymentInstructionMapper;
        this.txService = txService;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${payment.scheduled.poll-interval-ms:30000}")
    public void triggerDueScheduled() {
        List<PaymentInstruction> due = paymentInstructionMapper.selectDueScheduled();
        if (due.isEmpty()) {
            return;
        }
        for (PaymentInstruction pi : due) {
            try {
                boolean claimed = txService.claimScheduled(pi);
                if (!claimed) {
                    log.info("[SCHED] 이미 선점됨 skip piId={}", pi.getPaymentInstructionId());
                    continue;
                }
                log.info("[SCHED] claim 성공 piId={}", pi.getPaymentInstructionId());
                orchestrator.executeScheduledIntraBank(pi);
                log.info("[SCHED] 실행 완료 piId={}", pi.getPaymentInstructionId());
            } catch (Exception e) {
                // 실패 시 PROCESSING 잔류 → 3-C 에서 보상/FAILED 처리 추가 예정
                log.error("[SCHED] 처리 실패 piId={}", pi.getPaymentInstructionId(), e);
            }
        }
    }
}
