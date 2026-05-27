package com.bank.loan.schedule.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 원리금균등(EQUAL) 회차 계산기.
 *
 * 공식:
 *   monthly_rate (r) = annual_rate_bps / 120000  (= bps / 10000 / 12, 소수)
 *   pmt = P * r * (1+r)^n / ((1+r)^n - 1)         (r > 0)
 *   pmt = P / n                                   (r = 0)
 *
 * 각 회차:
 *   interest_i  = remaining_balance * r           (정수 원 단위 반올림)
 *   principal_i = pmt - interest_i                (정수 원 단위 반올림)
 *   remaining   -= principal_i
 *
 * 마지막 회차에서 누적 라운딩 오차를 흡수해 sum(principal) == 원금이 정확히 성립하게 한다.
 *
 * 반올림 정책: HALF_EVEN (banker's rounding) — 회차 합계 편향 최소화.
 *
 * BigDecimal 산술 정밀도: MathContext.DECIMAL64 (16자리) — 36개월 / 7~8% 수준에서 충분.
 */
public final class EqualPaymentCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal BPS_TO_DECIMAL = BigDecimal.valueOf(10_000);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private EqualPaymentCalculator() {}

    public record Installment(long scheduledPrincipal, long scheduledInterest, long remainingBalance) {
        public long scheduledTotal() {
            return scheduledPrincipal + scheduledInterest;
        }
    }

    public static List<Installment> calculate(long principal, int annualRateBps, int periodMonths) {
        if (principal <= 0) throw new IllegalArgumentException("principal must be > 0");
        if (annualRateBps < 0) throw new IllegalArgumentException("annualRateBps must be >= 0");
        if (periodMonths <= 0) throw new IllegalArgumentException("periodMonths must be > 0");

        BigDecimal r = BigDecimal.valueOf(annualRateBps)
                .divide(BPS_TO_DECIMAL.multiply(MONTHS_PER_YEAR), MC);
        BigDecimal p = BigDecimal.valueOf(principal);

        BigDecimal pmt = computeMonthlyPayment(p, r, periodMonths);

        List<Installment> result = new ArrayList<>(periodMonths);
        long remaining = principal;

        for (int i = 1; i <= periodMonths; i++) {
            long interestI;
            long principalI;

            if (i == periodMonths) {
                interestI = BigDecimal.valueOf(remaining).multiply(r, MC)
                        .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
                principalI = remaining;
            } else {
                interestI = BigDecimal.valueOf(remaining).multiply(r, MC)
                        .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
                BigDecimal principalBd = pmt.subtract(BigDecimal.valueOf(interestI));
                principalI = principalBd.setScale(0, RoundingMode.HALF_EVEN).longValueExact();
                if (principalI > remaining) principalI = remaining;
                if (principalI < 0) principalI = 0;
            }

            remaining -= principalI;
            result.add(new Installment(principalI, interestI, remaining));
        }

        return result;
    }

    private static BigDecimal computeMonthlyPayment(BigDecimal principal, BigDecimal r, int n) {
        if (r.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(n), MC);
        }
        BigDecimal one = BigDecimal.ONE;
        BigDecimal onePlusRPowN = one.add(r).pow(n, MC);
        BigDecimal numerator = principal.multiply(r, MC).multiply(onePlusRPowN, MC);
        BigDecimal denominator = onePlusRPowN.subtract(one, MC);
        return numerator.divide(denominator, MC);
    }
}
