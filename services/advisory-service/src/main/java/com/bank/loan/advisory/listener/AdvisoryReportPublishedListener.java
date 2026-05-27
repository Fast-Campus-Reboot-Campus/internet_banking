package com.bank.loan.advisory.listener;

import com.bank.loan.advisory.event.AdvisoryReportPublishedEvent;
import com.bank.loan.advisory.kafka.AdvisoryKafkaOutboxAppender;
import com.bank.loan.advisory.kafka.AdvisoryKafkaOutboxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 어드바이저리 리포트 발행 이벤트 → Kafka Outbox 적재.
 *
 * AdvisoryBatchEvaluationService(@Transactional) 안에서 발행되므로
 * AFTER_COMMIT 후 REQUIRES_NEW 트랜잭션으로 Outbox에 적재한다.
 * 실패해도 배치 결과에 영향 없음 — 예외 격리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdvisoryReportPublishedListener {

    public static final String EVENT_TYPE = "REPORT_PUBLISHED";

    private final AdvisoryKafkaOutboxAppender kafkaOutboxAppender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportPublished(AdvisoryReportPublishedEvent event) {
        try {
            kafkaOutboxAppender.enqueue(
                    EVENT_TYPE,
                    String.valueOf(event.advrId()),
                    AdvisoryKafkaOutboxMessage.TOPIC_REPORT,
                    String.valueOf(event.advrId()),
                    buildPayload(event));
            log.debug("[report-kafka] enqueued — advrId={} reviewer={} severity={}",
                    event.advrId(), event.targetReviewerId(), event.severityCd());
        } catch (Exception e) {
            log.error("[report-kafka] outbox 적재 실패 (무시) — advrId={}: {}",
                    event.advrId(), e.getMessage());
        }
    }

    private static String buildPayload(AdvisoryReportPublishedEvent e) {
        return String.format(
                "{\"advrId\":%d,\"targetReviewerId\":%s,\"severityCd\":\"%s\",\"baseDate\":\"%s\"}",
                e.advrId(),
                e.targetReviewerId() != null ? e.targetReviewerId().toString() : "null",
                e.severityCd(),
                e.baseDate());
    }
}
