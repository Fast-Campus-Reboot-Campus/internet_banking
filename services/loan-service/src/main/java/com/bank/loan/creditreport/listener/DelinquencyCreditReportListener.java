package com.bank.loan.creditreport.listener;

import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import com.bank.loan.delinquency.domain.Delinquency;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.DelinquencyOpenedEvent;
import com.bank.loan.notification.event.DelinquencyResolvedEvent;
import com.bank.loan.notification.event.DelinquencyStageAdvancedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 연체 라이프사이클 이벤트 → KCB/NICE 자동 신고 발화.
 *
 * 실행 모델:
 *   - AFTER_COMMIT : rollover 트랜잭션이 commit 되어 dlq row 가 영속화된 뒤 시작
 *   - @Async       : 신고 적재가 rollover 본 트랜잭션을 지연시키지 않게 별도 스레드
 *
 * 멱등은 CreditInfoReportService.submit(cntrId, dlqId, req) 가 (cntrId, dlqId, type, reason) 키로 보장.
 * STAGE_ADVANCED 는 STAGE_2/STAGE_3 진입에만 의미 — STAGE_0/1 내부 단계 전이는 신고 안 함.
 *
 * 발신 기관(agency) 은 본 단계에서 시스템 디폴트 KCB. 상품별 정책 차등은 후속.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelinquencyCreditReportListener {

    private static final String DEFAULT_AGENCY = "KCB";

    private static final String TYPE_DELINQUENCY = "DELINQUENCY";
    private static final String TYPE_RESOLUTION  = "RESOLUTION";

    private static final String TARGET_EXISTING = "EXISTING";

    private static final String REASON_OPENED          = "DELINQUENCY_OPENED";
    private static final String REASON_STAGE_ADVANCED  = "DELINQUENCY_STAGE_ADVANCED";
    private static final String REASON_RESOLVED        = "DELINQUENCY_RESOLVED";

    private final CreditInfoReportService reportService;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpened(DelinquencyOpenedEvent event) {
        String payload = String.format(
                "{\"dlqId\":%d,\"dlqStartDate\":\"%s\",\"dlqStageCd\":\"%s\"}",
                event.dlqId(), event.dlqStartDate(), event.dlqStageCd());
        SubmitReportRequest req = new SubmitReportRequest(
                TYPE_DELINQUENCY, DEFAULT_AGENCY, TARGET_EXISTING, REASON_OPENED, payload);
        reportService.submit(event.cntrId(), event.dlqId(), req);
        log.info("[creditreport] auto-emit OPENED cntrId={} dlqId={}", event.cntrId(), event.dlqId());
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStageAdvanced(DelinquencyStageAdvancedEvent event) {
        // STAGE_0/STAGE_1 은 내부 단계 — 외부 신고 대상 아님.
        if (!Delinquency.STAGE_2.equals(event.toStage())
                && !Delinquency.STAGE_3.equals(event.toStage())) {
            return;
        }
        String payload = String.format(
                "{\"dlqId\":%d,\"fromStage\":\"%s\",\"toStage\":\"%s\",\"dlqDays\":%d}",
                event.dlqId(), event.fromStage(), event.toStage(), event.dlqDays());
        SubmitReportRequest req = new SubmitReportRequest(
                TYPE_DELINQUENCY, DEFAULT_AGENCY, TARGET_EXISTING, REASON_STAGE_ADVANCED, payload);
        reportService.submit(event.cntrId(), event.dlqId(), req);
        log.info("[creditreport] auto-emit STAGE_ADVANCED cntrId={} dlqId={} {}→{}",
                event.cntrId(), event.dlqId(), event.fromStage(), event.toStage());
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResolved(DelinquencyResolvedEvent event) {
        String payload = String.format(
                "{\"dlqId\":%d,\"resolvedAt\":\"%s\"}",
                event.dlqId(), event.resolvedAt());
        SubmitReportRequest req = new SubmitReportRequest(
                TYPE_RESOLUTION, DEFAULT_AGENCY, TARGET_EXISTING, REASON_RESOLVED, payload);
        reportService.submit(event.cntrId(), event.dlqId(), req);
        log.info("[creditreport] auto-emit RESOLVED cntrId={} dlqId={}", event.cntrId(), event.dlqId());
    }
}
