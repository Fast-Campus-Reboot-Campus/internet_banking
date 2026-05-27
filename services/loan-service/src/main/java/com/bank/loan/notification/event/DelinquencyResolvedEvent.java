package com.bank.loan.notification.event;

import java.time.OffsetDateTime;

/**
 * 연체 해소 (ACTIVE → RESOLVED) 이벤트.
 *
 * 모든 OVERDUE 회차가 사라져 dlq 가 닫힌 시점에 발화된다.
 * creditreport listener 는 RESOLUTION/RESOLVED 자동 신고를 적재한다.
 *
 *   cntrId      약정 PK
 *   dlqId       연체 row PK
 *   resolvedAt  해소 처리 시각 (rollover 트랜잭션 시점)
 */
public record DelinquencyResolvedEvent(
        Long cntrId,
        Long dlqId,
        OffsetDateTime resolvedAt
) {
}
