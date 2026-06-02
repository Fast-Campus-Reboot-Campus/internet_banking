package com.bank.ai.rag.retriever;

import com.bank.ai.langfuse.LangfuseService;
import com.bank.ai.rag.ingestion.embedder.EmbeddingClient;
import com.bank.ai.rag.observability.RagMetrics;
import com.bank.ai.rag.retriever.dto.RagChunkHit;
import com.bank.ai.rag.retriever.dto.RagSearchRequest;
import com.bank.ai.rag.retriever.dto.RagSearchResponse;
import com.bank.ai.rag.store.VectorConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RAG 검색 오케스트레이터.
 *
 * 흐름:
 * 1. profile → 허용 docType 목록 결정
 * 2. query 텍스트 → 임베딩 벡터 (EmbeddingClient)
 * 3. 벡터 리터럴 → ChunkSearchRepository 로 pgvector 유사도 검색
 * 4. RagSearchResponse 반환
 *
 * 임베딩 호출은 @Transactional 바깥에서 수행한다 (AI_GUIDELINES).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieverService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K     = 50;

    private final EmbeddingClient       embeddingClient;
    private final ChunkSearchRepository searchRepository;
    private final RagMetrics            ragMetrics;

    @Autowired(required = false)
    private LangfuseService langfuse;

    /**
     * @param req 검색 요청 (query, profile, sensitivityCd, asOfDate, topK)
     * @return 유사도 높은 청크 목록 (score 내림차순)
     */
    public RagSearchResponse search(RagSearchRequest req) {
        if (req.query() == null || req.query().isBlank()) {
            throw new IllegalArgumentException("query 는 비어있을 수 없습니다.");
        }

        RagProfile profile = RagProfile.fromCode(req.profile());
        int        topK    = resolveTopK(req.topK());

        log.debug("[retriever] profile={} topK={} asOfDate={}", profile.getCode(), topK, req.asOfDate());

        long started = System.nanoTime();
        Instant startTime = Instant.now();
        try {
            // ── 임베딩 (외부 API, 트랜잭션 바깥) ──────────────────────────
            float[] vector    = embeddingClient.embed(List.of(req.query())).get(0);
            String  vecStr    = VectorConverter.toVectorString(vector);

            // ── 벡터 유사도 검색 ──────────────────────────────────────────
            List<RagChunkHit> hits = searchRepository.findNearest(
                    vecStr, profile.getDocTypes(), req.asOfDate(), topK);

            log.debug("[retriever] hits={}", hits.size());

            if (langfuse != null) {
                String traceId = langfuse.newTraceId();
                langfuse.trace(traceId, "ai-service", null);
                langfuse.span(traceId, "rag-search",
                        Map.of("query", req.query(), "profile", profile.getCode()),
                        Map.of("hitCount", hits.size()),
                        startTime, Instant.now());
            }

            return new RagSearchResponse(req.query(), profile.getCode(), hits);
        } finally {
            ragMetrics.recordSearch(profile.getCode(),
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private int resolveTopK(Integer requested) {
        if (requested == null || requested <= 0) return DEFAULT_TOP_K;
        return Math.min(requested, MAX_TOP_K);
    }
}
