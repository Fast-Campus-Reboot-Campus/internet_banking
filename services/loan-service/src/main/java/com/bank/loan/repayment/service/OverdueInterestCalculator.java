package com.bank.loan.repayment.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 회차당 연체이자 산정기.
 *
 * 공식:
 *   overdueInterest = overdueBase × overdueRateBps × days / 10000 / 365
 *
 *   overdueBase     연체 적용 기준 금액 (단순화: 회차 미납 원금)
 *   overdueRateBps  연체 가산금리 (bps)
 *   days            연체 일수 (due_date 다음날부터 baseDate 까지)
 *
 * 정수 원 단위 반올림: HALF_EVEN (banker's rounding) — EqualPaymentCalculator 와 동일 정책.
 * 입력이 양수가 아니면 0 반환.
 *
 * 한계 (의식적 단순화 — 본 단계):
 *   - day_count_basis 는 ACT/365 고정. 상품별 ACT/360, 30/360 등 차등은 후속.
 *   - 단리 가정. 복리 가산은 도입 X.
 *   - overdueBase 정밀화(미납 원금 - 누적 갚힌 원금) 는 호출자 책임 — 본 단계는 단순화로
 *     scheduled_principal 그대로 사용 가능.
 */
public final class OverdueInterestCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal BPS_TO_DECIMAL = BigDecimal.valueOf(10_000);
    private static final BigDecimal DAYS_PER_YEAR  = BigDecimal.valueOf(365);

    private OverdueInterestCalculator() {}

    public static long compute(long overdueBase, int overdueRateBps, int days) {
        if (overdueBase <= 0 || overdueRateBps <= 0 || days <= 0) return 0L;
        return BigDecimal.valueOf(overdueBase)
                .multiply(BigDecimal.valueOf(overdueRateBps), MC)
                .multiply(BigDecimal.valueOf(days), MC)
                .divide(BPS_TO_DECIMAL.multiply(DAYS_PER_YEAR), MC)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
    }
}
