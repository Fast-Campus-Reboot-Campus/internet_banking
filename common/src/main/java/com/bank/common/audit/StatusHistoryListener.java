package com.bank.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;

/**
 * StatusChangeEvent 를 받아 도메인 트랜잭션의 commit 직전(BEFORE_COMMIT)에
 * status_history INSERT 를 같은 트랜잭션 내에서 수행한다.
 *
 *  - 도메인 변경과 이력 기록이 원자적으로 동일 트랜잭션에서 처리됨
 *  - 도메인 트랜잭션 롤백 시 이력도 함께 롤백됨
 *  - changedAt 미지정 시 listener 가 현재 시각으로 보정
 *
 * AOP 가 아닌 명시적 이벤트 발행 패턴을 채택한 이유:
 *  - 사유코드·비고·actor 같은 컨텍스트를 메서드 시그니처로 강제 → 테스트·감사 추적 용이
 *  - "어떤 setter 가 상태 변경인지" 의 컨벤션·어노테이션 의존 제거
 */
@Slf4j
@Configuration
@ConditionalOnClass(StatusHistoryRepository.class)
@RequiredArgsConstructor
public class StatusHistoryListener {

    private final StatusHistoryRepository repository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onStatusChange(StatusChangeEvent event) {
        StatusHistory row = StatusHistory.builder()
                .targetDomainCd(event.targetDomainCd())
                .targetTableCd(event.targetTableCd())
                .targetId(event.targetId())
                .beforeStatusCd(event.beforeStatusCd())
                .afterStatusCd(event.afterStatusCd())
                .changeReasonCd(event.changeReasonCd())
                .changeRemark(event.changeRemark())
                .changedAt(event.changedAt() == null ? OffsetDateTime.now() : event.changedAt())
                .changedBy(event.changedBy())
                .build();
        repository.save(row);
    }
}
