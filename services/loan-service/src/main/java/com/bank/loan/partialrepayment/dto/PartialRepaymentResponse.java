package com.bank.loan.partialrepayment.dto;

import com.bank.loan.repayment.domain.RepaymentTransaction;
import com.bank.loan.schedule.domain.RepaymentSchedule;

import java.time.OffsetDateTime;

/**
 * 부분상환 결과.
 *
 *   rtxId                생성된 RepaymentTransaction PK
 *   rschId               대상 회차 PK
 *   installmentNo        대상 회차 번호
 *   paidAmount           이번에 납부한 금액
 *   principalPortion     원금 분배분 (비례 분배 — 본 단계 단순화)
 *   interestPortion      이자 분배분
 *   cumulativePaid       회차 누적 납부합 (이번 거래 포함)
 *   scheduledTotal       회차 예정 총액
 *   scheduleStatusAfter  처리 후 회차 상태 (PARTIAL_PAID 또는 PAID)
 *   paidAt               처리 시각
 */
public record PartialRepaymentResponse(
        Long rtxId,
        Long rschId,
        Integer installmentNo,
        Long paidAmount,
        Long principalPortion,
        Long interestPortion,
        Long cumulativePaid,
        Long scheduledTotal,
        String scheduleStatusAfter,
        OffsetDateTime paidAt
) {
    public static PartialRepaymentResponse of(RepaymentTransaction tx, RepaymentSchedule schedule,
                                              long cumulativePaid) {
        return new PartialRepaymentResponse(
                tx.getRtxId(),
                schedule.getRschId(),
                schedule.getInstallmentNo(),
                tx.getTotalAmount(),
                tx.getPrincipalAmount(),
                tx.getInterestAmount(),
                cumulativePaid,
                schedule.getScheduledTotal(),
                schedule.currentStatus(),
                tx.getPaidAt()
        );
    }
}
