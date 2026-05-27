package com.bank.loan.notification.event;

/**
 * 본심사 승인 도메인 이벤트.
 * LoanReviewService 최초 결정에서 APPROVED 일 때만 발행.
 * 정정/재심사는 본 단계 알림 대상 외.
 */
public record LoanApprovedEvent(
        Long applId,
        Long revId,
        Long customerId,
        Long approvedAmount
) {
}
