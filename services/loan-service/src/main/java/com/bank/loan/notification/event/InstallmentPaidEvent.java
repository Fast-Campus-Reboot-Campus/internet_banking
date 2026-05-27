package com.bank.loan.notification.event;

/**
 * 회차 상환 완료 도메인 이벤트.
 * RepaymentService.repayInstallment 트랜잭션 commit 후 발행.
 *
 *   channelCd  자동이체(AUTO_DEBIT) / 수동(MANUAL) 등 — 리스너가 알림 문구 분기에 활용
 */
public record InstallmentPaidEvent(
        Long rtxId,
        Long cntrId,
        Long rschId,
        Integer installmentNo,
        Long paidAmount,
        String channelCd
) {
}
