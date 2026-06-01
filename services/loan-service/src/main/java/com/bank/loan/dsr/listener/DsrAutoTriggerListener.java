package com.bank.loan.dsr.listener;

import com.bank.loan.creditevaluation.event.CreditEvaluationCompletedEvent;
import com.bank.loan.dsr.dto.RunDsrCalculationRequest;
import com.bank.loan.dsr.service.DsrCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 신용평가 완료 후 DSR 산출을 자동 실행한다.
 *
 * - annualIncomeAmt 는 신청 시 입력한 추정 연소득 사용.
 *   기존 부채 정보 미보유 시 0 으로 처리 — 보수적 산정은 estimateNewAnnualRepay 가 담당.
 * - 실패해도 신용평가 결과에 영향 없음 — 로그만 기록.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DsrAutoTriggerListener {

    private static final String DSR_ENGINE_VERSION = "AUTO_DSR_STUB";

    private final DsrCalculationService dsrCalculationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreditEvaluationCompleted(CreditEvaluationCompletedEvent event) {
        try {
            long annualIncome = event.annualIncomeAmt() != null ? event.annualIncomeAmt() : 0L;

            RunDsrCalculationRequest req = new RunDsrCalculationRequest(
                    annualIncome,
                    null,
                    null,
                    null,
                    null,
                    "AUTO",
                    DSR_ENGINE_VERSION,
                    null
            );

            dsrCalculationService.run(event.applId(), req);
            log.info("DSR 자동 산출 완료: applId={} cevalDecision={}", event.applId(), event.cevalDecisionCd());
        } catch (Exception e) {
            log.warn("DSR 자동 산출 실패 (무시) applId={}: {}", event.applId(), e.getMessage());
        }
    }
}
