package com.bank.loan.ecl.engine;

import com.bank.loan.ecl.domain.LoanEclSummary;
import com.bank.loan.delinquency.domain.Delinquency;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * {@link EclEngine} 기본 mock 구현 — 외부 통계 엔진 없이 단순 룰로 PD/LGD 결정.
 *
 * 활성 조건: {@code loan.ecl.engine.type} 가 {@code mock} 이거나 미설정.
 *
 * PD (bps, 10000=100%):
 *   비연체 ACTIVE                 → IFRS Stage 1, PD =    50 (0.5%)
 *   연체 STAGE_0 (1~4일)           → IFRS Stage 2, PD =   200 (2%)
 *   연체 STAGE_1 (5~29일)          → IFRS Stage 2, PD =   500 (5%)
 *   연체 STAGE_2 (30~89일)         → IFRS Stage 3, PD =  3000 (30%)
 *   연체 STAGE_3 (90일+)           → IFRS Stage 3, PD =  6000 (60%)
 *
 * LGD (bps):
 *   보증보험 ISSUED → 1000 (10%)
 *   담보 등록      → 3000 (30%)
 *   그 외          → 5000 (50%)
 *
 * EAD = 원금 잔액 그대로.
 *
 * ECL = EAD × PDbps × LGDbps / 10000 / 10000 (HALF_EVEN).
 *
 * 본 단계 한계 (의식적 단순화):
 *   - Lifetime PD vs 12-month PD 구분 없음 (Stage 따라 룰만 다름)
 *   - 차주 특성 (소득·신용등급) 반영 없음
 *   - 시간가치 (discount factor) 반영 없음
 */
@Component
@ConditionalOnProperty(name = "loan.ecl.engine.type", havingValue = "mock", matchIfMissing = true)
public class MockEclEngine implements EclEngine {

    public static final String ENGINE_VERSION = "MOCK-v1";

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal SCALE = BigDecimal.valueOf(10_000L * 10_000L);

    @Override
    public EclResult calculate(EclInput input) {
        String ifrsStage = ifrsStageOf(input.delinquencyStageCd());
        int pdBps  = pdOf(input.delinquencyStageCd());
        int lgdBps = lgdOf(input);
        long ead   = Math.max(0L, input.principalBalance());
        long ecl   = computeEcl(ead, pdBps, lgdBps);
        return new EclResult(ifrsStage, pdBps, lgdBps, ead, ecl, ENGINE_VERSION);
    }

    private static String ifrsStageOf(String dlqStage) {
        if (dlqStage == null)                          return LoanEclSummary.STAGE_1;
        if (Delinquency.STAGE_0.equals(dlqStage))      return LoanEclSummary.STAGE_2;
        if (Delinquency.STAGE_1.equals(dlqStage))      return LoanEclSummary.STAGE_2;
        if (Delinquency.STAGE_2.equals(dlqStage))      return LoanEclSummary.STAGE_3;
        if (Delinquency.STAGE_3.equals(dlqStage))      return LoanEclSummary.STAGE_3;
        return LoanEclSummary.STAGE_1;
    }

    private static int pdOf(String dlqStage) {
        if (dlqStage == null)                          return 50;
        if (Delinquency.STAGE_0.equals(dlqStage))      return 200;
        if (Delinquency.STAGE_1.equals(dlqStage))      return 500;
        if (Delinquency.STAGE_2.equals(dlqStage))      return 3000;
        if (Delinquency.STAGE_3.equals(dlqStage))      return 6000;
        return 50;
    }

    private static int lgdOf(EclInput input) {
        if (input.hasActiveGuaranteeInsurance()) return 1000;
        if (input.hasCollateral())               return 3000;
        return 5000;
    }

    private static long computeEcl(long ead, int pdBps, int lgdBps) {
        if (ead <= 0 || pdBps <= 0 || lgdBps <= 0) return 0L;
        return BigDecimal.valueOf(ead)
                .multiply(BigDecimal.valueOf(pdBps), MC)
                .multiply(BigDecimal.valueOf(lgdBps), MC)
                .divide(SCALE, MC)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
    }
}
