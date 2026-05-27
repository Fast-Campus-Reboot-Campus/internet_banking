package com.bank.loan.repayment.service;

/**
 * 상환 금액 분배 공통 헬퍼 — flows §2.2 순서: 연체이자 → 정상이자 → 원금. 수수료는 별도.
 *
 * 입력 {@code amount} 는 수수료를 제외한 받은 금액을 의미. 헬퍼는 amount 를
 *   연체이자 → 정상이자 → 원금 순으로 채우고, 남은 금액은 모두 원금에 귀속한다.
 * 수수료(requestedFee)는 amount 분배에 영향을 주지 않으며, 호출자가 tx.total_amount 산정 시
 *   amount + fee 형태로 합산할지 amount 안에 포함된 의미인지 선택한다.
 *
 * <pre>
 *   overdue   = min(amount, max(0, remainingOverdue))
 *   interest  = min(amount - overdue, max(0, remainingInterest))
 *   principal = amount - overdue - interest
 *   fee       = max(0, requestedFee)
 * </pre>
 *
 * 호출자가 {@code remainingOverdue}, {@code remainingInterest} 를 미리 계산해 넘긴다 —
 *   누적 지급분 차감(부분상환), 회차 상태별 0 처리(정상 회차) 등 도메인 결정은 호출자 책임.
 */
public final class PaymentAllocator {

    private PaymentAllocator() {}

    public record Allocation(long overdue, long interest, long principal, long fee) {
        public long totalDistributed() {
            return overdue + interest + principal;
        }
    }

    public static Allocation allocate(long amount,
                                      long remainingOverdue,
                                      long remainingInterest,
                                      long requestedFee) {
        if (amount < 0L) {
            throw new IllegalArgumentException("amount must be >= 0, got " + amount);
        }
        long roOverdue = Math.max(0L, remainingOverdue);
        long roInterest = Math.max(0L, remainingInterest);
        long fee = Math.max(0L, requestedFee);

        long overduePortion = Math.min(amount, roOverdue);
        long afterOverdue = amount - overduePortion;
        long interestPortion = Math.min(afterOverdue, roInterest);
        long principalPortion = afterOverdue - interestPortion;

        return new Allocation(overduePortion, interestPortion, principalPortion, fee);
    }
}
