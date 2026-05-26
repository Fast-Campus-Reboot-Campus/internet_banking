package com.bank.loan.notification.event;

/**
 * 대출 신청 접수 도메인 이벤트.
 * LoanApplicationService.create() 트랜잭션 commit 후 접수 안내 트리거.
 */
public record ApplicationSubmittedEvent(
        Long applId,
        String applNo,
        Long customerId,
        Long prodId
) {
}
