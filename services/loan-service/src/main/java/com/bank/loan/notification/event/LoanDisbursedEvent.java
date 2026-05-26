package com.bank.loan.notification.event;

/**
 * 자금 인출 완료 도메인 이벤트.
 * LoanExecutionService.drawdown 의 최초 인출(SIGNED → ACTIVE 전이) 시점에 발행.
 */
public record LoanDisbursedEvent(
        Long cntrId,
        String cntrNo,
        Long customerId,
        Long executedAmount
) {
}
