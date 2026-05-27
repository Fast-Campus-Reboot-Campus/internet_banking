package com.bank.loan.notification.event;

/**
 * 만기 도래 도메인 이벤트.
 *
 * MaturityBatchService 일배치 트랜잭션 commit 후 발행.
 * 후속 listener (알림 outbox 적재 등) 가 수신해 고객 통지를 트리거한다.
 *
 *   matId                 만기 row PK
 *   cntrId                약정 PK
 *   currentMaturityDate   도달한 만기일 (YYYYMMDD)
 *   extensionCount        누적 연장 횟수 (0 이면 원만기 도래)
 */
public record MaturityReachedEvent(
        Long matId,
        Long cntrId,
        String currentMaturityDate,
        Integer extensionCount
) {
}
