package com.bank.loan.prescreening.engine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link CreditScoreEngine} 기본 mock 구현 — 외부 시스템 없이 자체 룰로 응답.
 *
 * 활성 조건: {@code loan.credit-score.engine.type} 가 {@code mock} 이거나 미설정(기본).
 * 운영 환경에서 {@link HttpCreditScoreEngine} 으로 전환하려면 해당 property 를 {@code http} 로 변경.
 *
 * 룰:
 *   1) 소득 미제출/0 이하 → REJECT "INCOME_NOT_PROVIDED"
 *   2) 신청금액 &gt; 연소득 × 5 → REJECT "OVER_INCOME_5X"
 *   3) 그 외 → PASS
 *        score = baseline(고용형태) (EMPLOYEE 720 / SELF_EMPLOYED 660 / 그 외 620)
 *        grade = score 매핑 (700+ BBB · 650+ BB · 그 외 B)
 *        pdBps = (1000 - score) × 4 (점수가 높을수록 낮은 부도확률)
 *        estimatedLimitAmt = min(requestedAmount, income × 5)
 */
@Component
@ConditionalOnProperty(name = "loan.credit-score.engine.type", havingValue = "mock", matchIfMissing = true)
public class MockCreditScoreEngine implements CreditScoreEngine {

    public static final String ENGINE_VERSION = "MOCK-v1";

    private static final String REJECT_NO_INCOME      = "INCOME_NOT_PROVIDED";
    private static final String REJECT_OVER_INCOME_5X = "OVER_INCOME_5X";

    private static final int    INCOME_MULTIPLIER = 5;
    private static final int    SCORE_EMPLOYEE      = 720;
    private static final int    SCORE_SELF_EMPLOYED = 660;
    private static final int    SCORE_DEFAULT       = 620;
    private static final String EMP_EMPLOYEE      = "EMPLOYEE";
    private static final String EMP_SELF_EMPLOYED = "SELF_EMPLOYED";

    @Override
    public CreditScoreResult evaluate(CreditScoreRequest req) {
        Long income = req.estimatedIncomeAmt();
        if (income == null || income <= 0L) {
            return reject(REJECT_NO_INCOME);
        }
        long incomeCap = income * INCOME_MULTIPLIER;
        Long requested = req.requestedAmount();
        if (requested != null && requested > incomeCap) {
            return reject(REJECT_OVER_INCOME_5X);
        }

        int score = scoreFor(req.employmentTypeCd());
        String grade = gradeFor(score);
        int pdBps = (1000 - score) * 4;
        long limit = requested != null ? Math.min(requested, incomeCap) : incomeCap;

        return new CreditScoreResult(
                CreditScoreResult.DECISION_PASS,
                score, grade, pdBps,
                limit,
                null,
                ENGINE_VERSION
        );
    }

    private CreditScoreResult reject(String reasonCd) {
        return new CreditScoreResult(
                CreditScoreResult.DECISION_REJECT,
                null, null, null,
                null,
                reasonCd,
                ENGINE_VERSION
        );
    }

    private int scoreFor(String employmentTypeCd) {
        if (EMP_EMPLOYEE.equals(employmentTypeCd)) return SCORE_EMPLOYEE;
        if (EMP_SELF_EMPLOYED.equals(employmentTypeCd)) return SCORE_SELF_EMPLOYED;
        return SCORE_DEFAULT;
    }

    private String gradeFor(int score) {
        if (score >= 700) return "BBB";
        if (score >= 650) return "BB";
        return "B";
    }
}
