package com.bank.ai.rag.seed;

import com.bank.ai.llm.policy.InlinePolicyIndex;
import com.bank.ai.llm.policy.PolicyIndex;
import com.bank.ai.rag.embedding.EmbeddingClient;
import com.bank.ai.rule.config.RuleEngineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 정책 코퍼스 P1 seed — 기동 시 1회 실행 (멱등).
 *
 * <p>적재 대상:
 * <ul>
 *   <li>{@code ai.policy.inline} 8개 정책 → 8 chunks</li>
 *   <li>{@code ai.rule-engine.pd-threshold-matrix} 5개 셀 → 5 chunks</li>
 * </ul>
 * {@code ON CONFLICT DO NOTHING} 으로 중복 기동 시 재적재 없음.
 * {@code ai.rag.enabled=true} 환경에서만 활성.
 *
 * <p>주의: pgvector SQL 포함 — H2 환경에서는 ai.rag.enabled 기본값 false 로 실행 안 됨.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PolicyCorpusSeedLoader implements ApplicationRunner {

    static final String CORPUS = "policy_regulation";
    static final String EMBEDDING_MODEL = "text-embedding-005";
    static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 4, 1);

    private final JdbcClient jdbcClient;
    private final EmbeddingClient embeddingClient;
    private final InlinePolicyIndex policyIndex;
    private final RuleEngineProperties ruleEngineProps;

    /** Embedding API 호출 비동기 위임 — readiness probe 미통과 방지. */
    @Autowired
    @Qualifier("llmExecutor")
    private Executor executor;

    /**
     * 기동 즉시 반환 — 실제 seed 작업은 llmExecutor 에 위임.
     * ON CONFLICT DO NOTHING 으로 멱등하므로 재기동 시 재적재 없음.
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("PolicyCorpusSeedLoader: 정책 코퍼스 P1 seed 비동기 시작");
        CompletableFuture.runAsync(this::doSeed, executor)
                .exceptionally(e -> {
                    log.error("PolicyCorpusSeedLoader: seed 실패", e);
                    return null;
                });
    }

    private void doSeed() {
        log.info("PolicyCorpusSeedLoader: seed 작업 시작");
        int inserted = 0;
        inserted += seedInlinePolicies();
        inserted += seedMatrixCells();
        log.info("PolicyCorpusSeedLoader: seed 완료 — inserted(또는 이미 존재) {} chunks", inserted);
    }

    // ─────────────────────────────────────────────────────────────────────

    private int seedInlinePolicies() {
        int count = 0;
        for (var entry : policyIndex.inline().entrySet()) {
            String id = entry.getKey();
            PolicyIndex.PolicyEntry policy = entry.getValue();

            String chunkText = "[정책] %s — %s\n\n%s".formatted(policy.source(), id, policy.text());
            String summary = policy.text();
            String metadata = """
                    {"source":"%s","article_no":"%s","tags":["policy","inline"]}
                    """.formatted(policy.source(), id).trim();

            upsert(id, 0, chunkText, summary, metadata);
            count++;
        }
        log.debug("PolicyCorpusSeedLoader: inline 정책 {} 건 처리", count);
        return count;
    }

    private int seedMatrixCells() {
        int count = 0;
        for (var productEntry : ruleEngineProps.pdThresholdMatrix().entrySet()) {
            String product = productEntry.getKey();
            for (var segmentEntry : productEntry.getValue().entrySet()) {
                String segment = segmentEntry.getKey();
                double threshold = segmentEntry.getValue();

                String sourceId = "MATRIX_%s_%s".formatted(product, segment.toUpperCase());
                String chunkText = """
                        [정책 매트릭스] %s / %s
                        PD 임계치: %.3f
                        상품·세그먼트별 PD 임계치 상한 기준. 신용정책위원회 분기 의결 값.
                        적용: product=%s, segment=%s
                        """.formatted(product, segment, threshold, product, segment).trim();
                String summary = "%s %s PD 임계치 %.3f".formatted(product, segment, threshold);
                String metadata = """
                        {"source":"policy_matrix","product":"%s","segment":"%s","matrix_coord":{"product":"%s","segment":"%s"},"tags":["matrix","pd_threshold"]}
                        """.formatted(product, segment, product, segment).trim();

                upsert(sourceId, 0, chunkText, summary, metadata);
                count++;
            }
        }
        log.debug("PolicyCorpusSeedLoader: 매트릭스 셀 {} 건 처리", count);
        return count;
    }

    private void upsert(String sourceId, int chunkSeq,
                        String chunkText, String summary, String metadata) {
        float[] vec = embeddingClient.embed(chunkText);
        String vecLiteral = toVectorLiteral(vec);

        jdbcClient.sql("""
                INSERT INTO ai_embedding
                    (corpus, source_id, chunk_seq, chunk_text, chunk_summary,
                     embedding, embedding_model, metadata, fts_tokens,
                     effective_date, is_active)
                VALUES
                    (:corpus, :sourceId, :chunkSeq, :chunkText, :summary,
                     CAST(:embedding AS vector), :model, :metadata::jsonb,
                     to_tsvector('simple', :chunkText),
                     :effectiveDate, true)
                ON CONFLICT (corpus, source_id, chunk_seq, embedding_model) DO NOTHING
                """)
                .param("corpus", CORPUS)
                .param("sourceId", sourceId)
                .param("chunkSeq", chunkSeq)
                .param("chunkText", chunkText)
                .param("summary", summary)
                .param("embedding", vecLiteral)
                .param("model", EMBEDDING_MODEL)
                .param("metadata", metadata)
                .param("effectiveDate", EFFECTIVE_DATE)
                .update();
    }

    private static String toVectorLiteral(float[] vec) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
