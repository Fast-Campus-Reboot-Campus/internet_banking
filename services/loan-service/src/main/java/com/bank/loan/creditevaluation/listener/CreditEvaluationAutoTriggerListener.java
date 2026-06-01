package com.bank.loan.creditevaluation.listener;

import com.bank.loan.creditevaluation.dto.RunCreditEvaluationRequest;
import com.bank.loan.creditevaluation.service.CreditEvaluationService;
import com.bank.loan.prescreening.event.PrescreeningPassedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 가심사 PASS 후 CB 신용평가를 자동 실행한다.
 *
 * - 가심사 엔진(MockCreditScoreEngine)이 이미 score/grade/limit을 산출했으므로
 *   그 결과를 CB 입력으로 재사용해 신용평가 row를 자동 생성한다.
 * - 실제 KCB/NICE API 연동 시 이 리스너에서 외부 API를 호출하면 됨.
 * - 실패해도 가심사 결과에 영향 없음 — 로그만 기록.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditEvaluationAutoTriggerListener {

    private static final String CB_ENGINE = "AUTO_CB_STUB";

    private final CreditEvaluationService creditEvaluationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrescreeningPassed(PrescreeningPassedEvent event) {
        try {
            // 가심사 점수 기반으로 CB 결정 자동 산출
            String cevalDecision = deriveDecision(event.estimatedScore());

            RunCreditEvaluationRequest req = new RunCreditEvaluationRequest(
                    CB_ENGINE,
                    event.engineVersion(),
                    event.estimatedGrade(),
                    event.estimatedScore(),
                    scoreToPdBps(event.estimatedScore()),
                    cevalDecision,
                    event.estimatedLimitAmt(),
                    event.estimatedRateBps(),
                    null
            );

            creditEvaluationService.run(event.applId(), req);
            log.info("신용평가 자동 실행 완료: applId={} decision={}", event.applId(), cevalDecision);
        } catch (Exception e) {
            log.warn("신용평가 자동 실행 실패 (무시) applId={}: {}", event.applId(), e.getMessage());
        }
    }

    /** 가심사 점수 → CB 결정 변환 (실제 CB 연동 전 임시 규칙) */
    private String deriveDecision(Integer score) {
        if (score == null) return "REVIEW";
        if (score >= 700) return "APPROVE";
        if (score >= 500) return "REVIEW";
        return "REJECT";
    }

    /** 점수 → PD bps 추정 (실제 CB 연동 전 임시 역산) */
    private Integer scoreToPdBps(Integer score) {
        if (score == null) return null;
        // 점수 높을수록 PD 낮음: 900→10bps, 700→100bps, 500→500bps
        int pd = Math.max(10, (int) (10000 * Math.exp(-0.007 * score)));
        return Math.min(pd, 5000);
    }
}
