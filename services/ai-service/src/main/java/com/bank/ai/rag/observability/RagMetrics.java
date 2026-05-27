package com.bank.ai.rag.observability;

import com.bank.ai.rag.chunk.repository.RagChunkRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RAG 관측 메트릭 진입점.
 *
 * Prometheus 노출명 (Micrometer 가 dot→underscore 변환):
 *   rag_ingest_duration_seconds   — Ingestion 한 사이클 소요 (tag: docType, status)
 *   rag_search_duration_seconds   — Retriever 한 호출 소요   (tag: profile)
 *   rag_chunk_total               — 현재 적재된 청크 수
 *
 * Timer 의 tag 조합은 호출 시점에 생성·캐시(MeterRegistry 내부)되므로
 * 본 클래스는 builder 만 보관하지 않고 record 단위로 등록한다.
 */
@Component
@RequiredArgsConstructor
public class RagMetrics {

    static final String INGEST_TIMER_NAME = "rag.ingest.duration";
    static final String SEARCH_TIMER_NAME = "rag.search.duration";
    static final String CHUNK_GAUGE_NAME  = "rag.chunk.total";

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_SKIP    = "skip";
    public static final String STATUS_FAIL    = "fail";

    private final MeterRegistry       registry;
    private final RagChunkRepository  chunkRepository;

    @PostConstruct
    void registerGauges() {
        Gauge.builder(CHUNK_GAUGE_NAME, chunkRepository, r -> (double) r.count())
                .description("현재 적재된 RAG 청크 총 수")
                .register(registry);
    }

    /** Ingestion 한 사이클 결과 기록. docType 누락 시 "unknown" 으로 대체. */
    public void recordIngest(String docType, String status, Duration elapsed) {
        Timer.builder(INGEST_TIMER_NAME)
                .description("RAG ingestion 한 사이클 소요 시간")
                .tag("docType", safeTag(docType))
                .tag("status", safeTag(status))
                .register(registry)
                .record(elapsed);
    }

    /** Retriever 검색 한 호출 결과 기록. */
    public void recordSearch(String profile, Duration elapsed) {
        Timer.builder(SEARCH_TIMER_NAME)
                .description("RAG 검색 한 호출 소요 시간")
                .tag("profile", safeTag(profile))
                .register(registry)
                .record(elapsed);
    }

    private static String safeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
