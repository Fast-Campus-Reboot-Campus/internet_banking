package com.bank.ai.rag.observability;

import com.bank.ai.rag.chunk.repository.RagChunkRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RagMetrics 단위 테스트 — SimpleMeterRegistry 로 Timer/Gauge 동작 검증.
 *
 * Prometheus 노출명은 Micrometer 가 dot→underscore 변환하므로
 * 코드상 이름(rag.ingest.duration) 으로 조회.
 */
class RagMetricsTest {

    private MeterRegistry       registry;
    private RagChunkRepository  chunkRepository;
    private RagMetrics          metrics;

    @BeforeEach
    void setUp() {
        registry        = new SimpleMeterRegistry();
        chunkRepository = Mockito.mock(RagChunkRepository.class);
        metrics         = new RagMetrics(registry, chunkRepository);
    }

    @Test
    void recordIngest_는_docType_status_태그로_Timer_생성() {
        metrics.recordIngest("INTERNAL_RULE", RagMetrics.STATUS_SUCCESS, Duration.ofMillis(120));
        metrics.recordIngest("INTERNAL_RULE", RagMetrics.STATUS_SUCCESS, Duration.ofMillis(80));
        metrics.recordIngest("INTERNAL_RULE", RagMetrics.STATUS_SKIP,    Duration.ofMillis(5));

        Timer success = registry.find("rag.ingest.duration")
                .tag("docType", "INTERNAL_RULE").tag("status", "success").timer();
        Timer skip    = registry.find("rag.ingest.duration")
                .tag("docType", "INTERNAL_RULE").tag("status", "skip").timer();

        assertThat(success).isNotNull();
        assertThat(success.count()).isEqualTo(2);
        assertThat(success.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isBetween(199.0, 201.0);
        assertThat(skip).isNotNull();
        assertThat(skip.count()).isEqualTo(1);
    }

    @Test
    void recordIngest_는_docType이_null_이면_unknown_태그() {
        metrics.recordIngest(null, RagMetrics.STATUS_FAIL, Duration.ofMillis(10));

        Timer t = registry.find("rag.ingest.duration")
                .tag("docType", "unknown").tag("status", "fail").timer();
        assertThat(t).isNotNull();
        assertThat(t.count()).isEqualTo(1);
    }

    @Test
    void recordSearch_는_profile_태그로_Timer_생성() {
        metrics.recordSearch("review",  Duration.ofMillis(50));
        metrics.recordSearch("product", Duration.ofMillis(30));
        metrics.recordSearch("review",  Duration.ofMillis(70));

        Timer review = registry.find("rag.search.duration").tag("profile", "review").timer();
        Timer product = registry.find("rag.search.duration").tag("profile", "product").timer();

        assertThat(review).isNotNull();
        assertThat(review.count()).isEqualTo(2);
        assertThat(product).isNotNull();
        assertThat(product.count()).isEqualTo(1);
    }

    @Test
    void registerGauges_는_chunkRepository_count_를_노출() {
        Mockito.when(chunkRepository.count()).thenReturn(42L);
        metrics.registerGauges();

        Gauge gauge = registry.find("rag.chunk.total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(42.0);

        // 카운트가 변하면 다음 측정에 반영
        Mockito.when(chunkRepository.count()).thenReturn(100L);
        assertThat(gauge.value()).isEqualTo(100.0);
    }
}
