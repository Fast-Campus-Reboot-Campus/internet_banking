package com.bank.loan.rag.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 유사 케이스 outbox 디스패처 — Phase E (E3-4).
 *
 * <p>발행 대기(PENDING/FAILED) outbox 를 주기적으로 Kafka 토픽으로 발행 후 SENT 로 전이.
 * Kafka 메시지 키 = {@code rev-<aggregateId>} (Connect {@code key.ignore=false} 시 ES 문서 id).
 * 발행 실패는 {@link LoanReviewOutbox#markFailed} 백오프 후 재시도.
 *
 * <p>{@code ai.case-outbox.enabled=true} 시에만 활성.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.case-outbox", name = "enabled", havingValue = "true")
public class SimilarCaseOutboxDispatcher {

    private static final List<String> DISPATCHABLE =
            List.of(LoanReviewOutbox.STATUS_PENDING, LoanReviewOutbox.STATUS_FAILED);

    private final LoanReviewOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CaseOutboxProperties props;

    @Scheduled(fixedDelayString = "${ai.case-outbox.poll-interval-ms:5000}")
    @Transactional
    public void dispatch() {
        OffsetDateTime now = OffsetDateTime.now();
        var pending = repository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(DISPATCHABLE, now);
        if (pending.isEmpty()) return;

        int sent = 0;
        for (var outbox : pending.stream().limit(props.batchSize()).toList()) {
            String key = "rev-" + outbox.getAggregateId();
            try {
                kafkaTemplate.send(props.topic(), key, outbox.getPayload()).get();
                outbox.markSent(OffsetDateTime.now());
                sent++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                outbox.markFailed("interrupted", OffsetDateTime.now());
                repository.save(outbox);
                log.warn("[case-outbox] 디스패처 인터럽트 outboxId={}", outbox.getOutboxId(), e);
                return;
            } catch (Exception e) {
                outbox.markFailed(e.getMessage(), OffsetDateTime.now());
                log.warn("[case-outbox] 발행 실패 outboxId={} attempt={}",
                        outbox.getOutboxId(), outbox.getAttemptNo(), e);
            }
            repository.save(outbox);
        }
        log.info("[case-outbox] 디스패처 완료 — 대상 {} / 발행 {}", pending.size(), sent);
    }
}
