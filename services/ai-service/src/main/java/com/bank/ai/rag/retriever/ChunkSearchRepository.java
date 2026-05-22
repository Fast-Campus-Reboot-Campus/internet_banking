package com.bank.ai.rag.retriever;

import com.bank.ai.rag.retriever.dto.RagChunkHit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * pgvector 코사인 거리 검색 전담 JDBC 레포지토리.
 *
 * {@code <=>} 연산자(코사인 거리)를 사용해 rag_chunk 테이블을 스캔한다.
 * score = 1 − cosine_distance (1.0 이 완전 동일).
 *
 * docTypeCd 필터링은 {@link RagProfile} 이 결정하며,
 * asOfDate 는 effective_from ≤ date < effective_to 범위 문서만 포함한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChunkSearchRepository {

    private static final String SEARCH_SQL = """
            SELECT c.chunk_id,
                   c.doc_id,
                   c.chunk_seq,
                   c.content,
                   1 - (c.embedding <=> CAST(? AS vector)) AS score,
                   d.doc_type_cd,
                   d.title,
                   d.source_uri
            FROM   rag_chunk   c
            JOIN   rag_document d ON d.doc_id = c.doc_id
            WHERE  d.deleted_at IS NULL
              AND  d.doc_type_cd = ANY(?)
              AND  (? IS NULL
                    OR (    (d.effective_from IS NULL OR d.effective_from <= ?)
                        AND (d.effective_to   IS NULL OR d.effective_to   >  ?)))
            ORDER BY c.embedding <=> CAST(? AS vector)
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param queryVecStr  "[v1,v2,...]" 형식 벡터 리터럴
     * @param docTypes     허용 docTypeCd 목록 (RagProfile 에서 결정)
     * @param asOfDate     기준일 YYYYMMDD, null 이면 유효기간 필터 미적용
     * @param topK         반환 최대 건수
     */
    public List<RagChunkHit> findNearest(String queryVecStr, List<String> docTypes,
                                         String asOfDate, int topK) {
        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            Array docTypeArray = conn.createArrayOf("text", docTypes.toArray(String[]::new));
            try (var ps = conn.prepareStatement(SEARCH_SQL)) {
                ps.setString(1, queryVecStr);           // CAST(? AS vector) — ORDER 기준
                ps.setArray(2, docTypeArray);           // doc_type_cd = ANY(?)
                ps.setString(3, asOfDate);              // IS NULL 체크
                ps.setString(4, asOfDate);              // effective_from ≤ ?
                ps.setString(5, asOfDate);              // effective_to   > ?
                ps.setString(6, queryVecStr);           // CAST(? AS vector) — score 계산
                ps.setInt(7, topK);

                List<RagChunkHit> hits = new ArrayList<>();
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        hits.add(new RagChunkHit(
                                rs.getLong("chunk_id"),
                                rs.getLong("doc_id"),
                                rs.getString("doc_type_cd"),
                                rs.getString("title"),
                                rs.getString("source_uri"),
                                rs.getInt("chunk_seq"),
                                rs.getString("content"),
                                rs.getDouble("score")
                        ));
                    }
                }
                return hits;
            }
        });
    }
}
