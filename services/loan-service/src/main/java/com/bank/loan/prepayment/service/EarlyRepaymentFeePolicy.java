package com.bank.loan.prepayment.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 중도상환 수수료 정책.
 *
 * 산식 (잔여 기간 비례):
 *   fee = amount × DEFAULT_EARLY_REPAYMENT_FEE_BPS × remainingMonths / totalMonths / 10000
 *
 *   amount           중도상환 원금
 *   remainingMonths  약정 잔여 개월수 (= max(0, totalMonths - elapsedMonths))
 *   totalMonths      약정 총 개월수 (= contracted_period_mo)
 *
 * 반올림: HALF_EVEN. 잔여 기간 0 또는 bps 0 이면 0 반환.
 *
 * [검토필요] 본 단계 시스템 디폴트 150bps 단일값. 상품별 차등(LoanProduct 정책) 은 후속.
 *           실제 운영은 상품·잔존기간 구간별 차등 + 최대 한도 설정이 일반적.
 */
public final class EarlyRepaymentFeePolicy {

    /** 시스템 디폴트: 잔존기간 100% 기준 연 1.5% (150bps) — 잔존기간이 줄어들수록 비례 감소. */
    public static final int DEFAULT_EARLY_REPAYMENT_FEE_BPS = 150;

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal BPS_TO_DECIMAL = BigDecimal.valueOf(10_000);

    private EarlyRepaymentFeePolicy() {}

    public static long calculate(long amount, int totalMonths, int remainingMonths) {
        if (amount <= 0 || totalMonths <= 0 || remainingMonths <= 0) return 0L;
        int bps = DEFAULT_EARLY_REPAYMENT_FEE_BPS;
        if (bps <= 0) return 0L;
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(bps), MC)
                .multiply(BigDecimal.valueOf(remainingMonths), MC)
                .divide(BPS_TO_DECIMAL, MC)
                .divide(BigDecimal.valueOf(totalMonths), MC)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
    }
}
