package com.bank.ai.rag.api;

import com.bank.ai.privacy.PiiMaskingFilter;
import com.bank.ai.rag.api.dto.ChunkBatchItem;
import com.bank.ai.rag.embedding.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 청크 배치 임베딩·적재 서비스 — D3-1.
 *
 * <p>흐름:
 * <ol>
 *   <li>PII 고신뢰도 검사 — 감지 시 {@link com.bank.ai.privacy.PiiLeakageException} throw</li>
 *   <li>EmbeddingClient 로 벡터 생성</li>
 *   <li>ai_embedding UPSERT (ON CONFLICT DO UPDATE) — corpus+source_id+chunk_seq+model 유일</li>
 * </ol>
 *
 * <p>pgvector SQL 포함 — H2 환경에서는 사용 불가. 컨트롤러 계층만 단위 테스트.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingBatchService {

    static final String DEFAULT_MODEL = "text-embedding-005";

    private final JdbcClient jdbcClient;
    private final EmbeddingClient embeddingClient;
    private final PiiMaskingFilter piiMaskingFilter;

    /**
     * 청크 목록을 임베딩 후 ai_embedding 에 일괄 upsert.
     *
     * @return 처리 건수 (PII 오류 시 예외 throw)
     */
    public int upsertAll(List<ChunkBatchItem> items) {
        int count = 0;
        for (var item : items) {
            piiMaskingFilter.assertNoSensitivePii(
                    item.chunkText(), "batch corpus=%s sourceId=%s".formatted(item.corpus(), item.sourceId()));
            upsert(item);
            count++;
        }
        log.info("EmbeddingBatchService: {} 건 upsert 완료", count);
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────

    private void upsert(ChunkBatchItem item) {
        float[] vec = embeddingClient.embed(item.chunkText());
        String vecLiteral = toVectorLiteral(vec);
        String metaJson = toJsonb(item.metadata());

        jdbcClient.sql("""
                INSERT INTO ai_embedding
                    (corpus, source_id, chunk_seq, chunk_text, chunk_summary,
                     embedding, embedding_model, metadata, fts_tokens,
                     effective_date, is_active)
                VALUES
                    (:corpus, :sourceId, :chunkSeq, :chunkText, :summary,
                     CAST(:embedding AS vector), :model, :metadata::jsonb,
                     to_tsvector('simple', :chunkText), :effectiveDate, true)
                ON CONFLICT (corpus, source_id, chunk_seq, embedding_model) DO UPDATE SET
                    chunk_text    = EXCLUDED.chunk_text,
                    chunk_summary = EXCLUDED.chunk_summary,
                    embedding     = EXCLUDED.embedding,
                    metadata      = EXCLUDED.metadata,
                    fts_tokens    = EXCLUDED.fts_tokens,
                    updated_at    = now()
                """)
                .param("corpus", item.corpus())
                .param("sourceId", item.sourceId())
                .param("chunkSeq", item.chunkSeq())
                .param("chunkText", item.chunkText())
                .param("summary", item.summary())
                .param("embedding", vecLiteral)
                .param("model", DEFAULT_MODEL)
                .param("metadata", metaJson)
                .param("effectiveDate", LocalDate.now())
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

    private static String toJsonb(Map<String, Object> meta) {
        if (meta.isEmpty()) return "{}";
        var sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : meta.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(entry.getKey()).append("\":");
            Object v = entry.getValue();
            if (v instanceof String s) sb.append('"').append(s).append('"');
            else sb.append(v);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
