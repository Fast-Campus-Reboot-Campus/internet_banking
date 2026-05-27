package com.bank.loan.notification.event;

/**
 * 약정 체결 완료 도메인 이벤트.
 *
 * LoanContractService.create() 트랜잭션 commit 후 안내서 발송 트리거에 사용.
 * 발송은 ContractNotificationListener 가 별도 스레드(notificationExecutor) 에서 수행한다.
 *
 *   cntrId       약정 PK
 *   cntrNo       약정 번호 (사용자 식별용)
 *   applId       원본 신청 PK
 *   customerId   고객 PK (수신처 조회 base)
 */
public record ContractSignedEvent(
        Long cntrId,
        String cntrNo,
        Long applId,
        Long customerId
) {
}
