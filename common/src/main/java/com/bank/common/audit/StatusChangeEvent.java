package com.bank.common.audit;

import java.time.OffsetDateTime;

/**
 * 도메인 상태 변경 이벤트. StatusHistoryListener 가 같은 트랜잭션의 commit 직전(BEFORE_COMMIT)
 * 시점에 status_history 에 append 한다.
 *
 *   targetDomainCd  : 도메인 식별자 (LOAN / DEPOSIT / CUSTOMER / PAYMENT / MASTER)
 *   targetTableCd   : 변경 대상 테이블 코드 (예: LOAN_REVIEW, LOAN_CONTRACT)
 *   targetId        : 대상 행 PK
 *   beforeStatusCd  : 변경 전 상태 코드 (신규 생성이면 null 허용)
 *   afterStatusCd   : 변경 후 상태 코드 (필수)
 *   changeReasonCd  : 변경 사유 코드 (CODE_MASTER 참조)
 *   changeRemark    : 변경 비고
 *   changedBy       : 변경 주체 사용자 ID (CurrentActorProvider 로부터 주입 권장)
 *   changedAt       : 명시적 변경 시각. null 이면 listener 가 OffsetDateTime.now() 로 보정
 */
public record StatusChangeEvent(
        String targetDomainCd,
        String targetTableCd,
        Long targetId,
        String beforeStatusCd,
        String afterStatusCd,
        String changeReasonCd,
        String changeRemark,
        Long changedBy,
        OffsetDateTime changedAt
) {

    public static StatusChangeEvent of(String targetDomainCd, String targetTableCd, Long targetId,
                                       String beforeStatusCd, String afterStatusCd,
                                       String changeReasonCd, String changeRemark, Long changedBy) {
        return new StatusChangeEvent(targetDomainCd, targetTableCd, targetId,
                beforeStatusCd, afterStatusCd, changeReasonCd, changeRemark, changedBy, null);
    }
}
