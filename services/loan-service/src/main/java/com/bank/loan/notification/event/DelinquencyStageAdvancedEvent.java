package com.bank.loan.notification.event;

/**
 * 연체 stage 상향 전이 이벤트 (예: STAGE_1 → STAGE_2).
 *
 * 같은 dlqId 의 stage 가 직전 일배치 결과 대비 변동된 경우에만 발화된다.
 * creditreport listener 는 STAGE_2/STAGE_3 진입에 한해 STAGE_ADVANCED 신고를 적재한다.
 *
 *   cntrId     약정 PK
 *   dlqId      연체 row PK
 *   fromStage  전이 전 stage
 *   toStage    전이 후 stage
 *   dlqDays    오늘자 연체 일수
 */
public record DelinquencyStageAdvancedEvent(
        Long cntrId,
        Long dlqId,
        String fromStage,
        String toStage,
        int dlqDays
) {
}
