package com.bank.loan.rag.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * CaseOutboxLagMonitor 단위 테스트 — Phase E (E4-1).
 */
@ExtendWith(MockitoExtension.class)
class CaseOutboxLagMonitorTest {

    @Mock
    private LoanReviewOutboxRepository repository;

    private SimpleMeterRegistry registry;
    private CaseOutboxLagMonitor monitor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        monitor = new CaseOutboxLagMonitor(repository, registry);
        monitor.registerGauge();
    }

    @Test
    void PENDING_없으면_lag_0() {
        when(repository.findTopByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Optional.empty());

        monitor.update();

        Gauge gauge = registry.find("rag.index.lag.seconds")
                .tag("corpus", "similar_cases").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    void PENDING_있으면_lag_양수() {
        LoanReviewOutbox oldest = LoanReviewOutbox.caseIndexed(1L, "{}");
        // 1분 전 생성된 것처럼 createdAt 설정 — @PrePersist 가 없으므로 리플렉션
        setCreatedAt(oldest, OffsetDateTime.now().minusMinutes(1));

        when(repository.findTopByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Optional.of(oldest));

        monitor.update();

        Gauge gauge = registry.find("rag.index.lag.seconds")
                .tag("corpus", "similar_cases").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isGreaterThanOrEqualTo(59.0); // 1분 - 약간의 오차 허용
    }

    @Test
    void gauge_항상_음수_없음() {
        // createdAt 이 미래인 경우에도 0으로 clamping
        LoanReviewOutbox future = LoanReviewOutbox.caseIndexed(2L, "{}");
        setCreatedAt(future, OffsetDateTime.now().plusMinutes(10));
        when(repository.findTopByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Optional.of(future));

        monitor.update();

        Gauge gauge = registry.find("rag.index.lag.seconds")
                .tag("corpus", "similar_cases").gauge();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private static void setCreatedAt(LoanReviewOutbox outbox, OffsetDateTime value) {
        try {
            var f = LoanReviewOutbox.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(outbox, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
