package com.bank.payment.scheduler;

import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.domain.mapper.PaymentInstructionMapper;
import com.bank.payment.domain.service.PaymentTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 예약이체 실행 폴링워커.
 *
 * scheduled_execution_at < now AND status='SCHEDULED' 인 PI = 실행 시각 도래한 예약이체.
 * 단계 2: claim(SCHEDULED→PROCESSING 선점) 까지만. 실행(B-3 출금/분개/완결)은 단계 3에서 추가.
 * ★ @Transactional 없음 — 건별 claimScheduled 가 독립 TX(선점 실패 시 SCHEDULED 복귀 보장).
 */
@Slf4j
@Component
public class ScheduledPaymentWorker {

    private final PaymentInstructionMapper paymentInstructionMapper;
    private final PaymentTransactionService txService;

    public ScheduledPaymentWorker(PaymentInstructionMapper paymentInstructionMapper,
                                  PaymentTransactionService txService) {
        this.paymentInstructionMapper = paymentInstructionMapper;
        this.txService = txService;
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
                // 단계 3에서 여기에 executeScheduled(pi) 추가 예정. 지금은 claim 까지만.
                log.info("[SCHED] claim 성공 piId={}", pi.getPaymentInstructionId());
            } catch (Exception e) {
                log.error("[SCHED] claim 처리 실패 piId={}", pi.getPaymentInstructionId(), e);
            }
        }
    }
}
