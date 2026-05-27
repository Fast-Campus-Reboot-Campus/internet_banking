package com.bank.loan.creditreport.listener;

import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import com.bank.loan.notification.config.AsyncConfig;
import com.bank.loan.notification.event.ContractSignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 약정 체결 → KCB/NICE 신규대출(NEW_LOAN) 자동 신고.
 *
 *   AFTER_COMMIT: 약정 트랜잭션 commit 후 한 번
 *   @Async      : 본 트랜잭션과 분리
 *
 * dlqId 가 없으므로 자동 발화 멱등 가드 비대상.
 * 약정은 cntrId 가 새로 생성되어야만 ContractSignedEvent 가 publish 되므로 운영상 중복 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractCreditReportListener {

    private static final String DEFAULT_AGENCY = "KCB";
    private static final String TYPE_NEW_LOAN  = "NEW_LOAN";
    private static final String TARGET_NEW     = "NEW";
    private static final String REASON_CONTRACTED = "NEW_LOAN_CONTRACTED";

    private final CreditInfoReportService reportService;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractSigned(ContractSignedEvent event) {
        String payload = String.format(
                "{\"cntrId\":%d,\"cntrNo\":\"%s\",\"applId\":%d,\"customerId\":%d}",
                event.cntrId(), event.cntrNo(), event.applId(), event.customerId());
        SubmitReportRequest req = new SubmitReportRequest(
                TYPE_NEW_LOAN, DEFAULT_AGENCY, TARGET_NEW, REASON_CONTRACTED, payload);
        reportService.submit(event.cntrId(), req);
        log.info("[creditreport] auto-emit NEW_LOAN cntrId={} cntrNo={}", event.cntrId(), event.cntrNo());
    }
}
