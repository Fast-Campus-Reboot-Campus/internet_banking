package com.bank.loan.prepayment.dto;

import com.bank.loan.repayment.domain.RepaymentTransaction;

import java.time.OffsetDateTime;

/**
 * 중도상환 결과 + 잔여 스케줄 요약.
 *
 *   rtxId                   생성된 RepaymentTransaction PK
 *   prepaidPrincipal        실제 처리된 원금 (사용자 입력 amount)
 *   feeAmount               중도상환 수수료 (잔여기간 비례)
 *   totalAmount             청구 총액 (prepaidPrincipal + feeAmount)
 *   outstandingAfter        처리 후 남은 원금
 *   supersededInstallments  SUPERSEDED 로 전이된 기존 회차 수
 *   newScheduleVersionCd    새 버전 코드 (예: V2). 새 회차가 없으면 null.
 *   newInstallmentCount     새 버전에 생성된 회차 수
 */
public record PrepaymentResponse(
        Long rtxId,
        Long cntrId,
        Long prepaidPrincipal,
        Long feeAmount,
        Long totalAmount,
        Long outstandingAfter,
        int supersededInstallments,
        String newScheduleVersionCd,
        int newInstallmentCount,
        OffsetDateTime paidAt
) {
    public static PrepaymentResponse of(RepaymentTransaction tx, long outstandingAfter,
                                        int superseded, String newVersionCd, int newCount) {
        return new PrepaymentResponse(
                tx.getRtxId(), tx.getCntrId(),
                tx.getPrincipalAmount(),
                tx.getFeeAmount(),
                tx.getTotalAmount(),
                outstandingAfter, superseded,
                newCount == 0 ? null : newVersionCd, newCount,
                tx.getPaidAt()
        );
    }
}
