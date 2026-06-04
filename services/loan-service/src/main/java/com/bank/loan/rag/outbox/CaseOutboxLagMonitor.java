package com.bank.loan.rag.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 케이스 outbox 인덱스 lag 게이지 — Phase E (E4-1).
 *
 * <p>{@code rag.index.lag.seconds{corpus=similar_cases}} Gauge 를 Prometheus 에 노출.
 * 값 = 현재 시각 − PENDING 상태 최고령 레코드의 {@code created_at}.
 * PENDING 건이 없으면 0.
 *
 * <p>30초 주기로 갱신. Grafana 에서 {@code rag_index_lag_seconds} 패널로 outbox 처리 지연을 모니터링.
 *
 * <p>{@code ai.case-outbox.enabled=true} 시에만 활성.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.case-outbox", name = "enabled", havingValue = "true")
public class CaseOutboxLagMonitor {

    private static final String CORPUS = "similar_cases";

    private final LoanReviewOutboxRepository repository;
    private final MeterRegistry registry;

    /** Gauge 값 보관 — 초 단위 lag. */
    private final AtomicLong lagSeconds = new AtomicLong(0L);

    @PostConstruct
    void registerGauge() {
        Gauge.builder("rag.index.lag.seconds", lagSeconds, AtomicLong::doubleValue)
                .tag("corpus", CORPUS)
                .description("케이스 outbox 최고령 PENDING 건 처리 지연(초) — 0이면 적체 없음")
                .register(registry);
        log.debug("[case-outbox-lag] gauge 등록 완료");
    }

    @Scheduled(fixedDelay = 30_000)
    void update() {
        repository.findTopByStatusOrderByCreatedAtAsc(LoanReviewOutbox.STATUS_PENDING)
                .ifPresentOrElse(
                        oldest -> {
                            long lag = Duration.between(oldest.getCreatedAt(), OffsetDateTime.now())
                                    .toSeconds();
                            lagSeconds.set(Math.max(0L, lag));
                            log.trace("[case-outbox-lag] lag={}s outboxId={}",
                                    lag, oldest.getOutboxId());
                        },
                        () -> lagSeconds.set(0L)
                );
    }
}
