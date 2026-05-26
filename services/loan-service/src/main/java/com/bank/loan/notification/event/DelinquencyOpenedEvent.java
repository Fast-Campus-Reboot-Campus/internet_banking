package com.bank.loan.notification.event;

/**
 * 연체 신규 발생 (ACTIVE 전이) 도메인 이벤트.
 *
 * DelinquencyRolloverService 일배치 트랜잭션 commit 후
 * creditreport 측 listener 가 DELINQUENCY/OPENED 신고를 자동 적재한다.
 *
 *   cntrId        약정 PK
 *   dlqId         연체 row PK
 *   dlqStartDate  연체 시작일 (YYYYMMDD)
 *   dlqStageCd    개시 시점 stage (STAGE_0/1/2/3)
 */
public record DelinquencyOpenedEvent(
        Long cntrId,
        Long dlqId,
        String dlqStartDate,
        String dlqStageCd
) {
}
