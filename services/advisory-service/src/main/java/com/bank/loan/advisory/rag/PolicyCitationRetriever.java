package com.bank.loan.advisory.rag;

import com.bank.loan.advisory.domain.AdvisoryRetrievalLog;
import com.bank.loan.advisory.dto.PolicyCitationResponse;
import com.bank.loan.advisory.repository.AdvisoryRetrievalLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 정책문서 청크 코사인 유사도 검색 (plan §11.4.2 — Task 6-6).
 *
 * CRITICAL 룰 발화 시 AdvisoryEvaluator 가 자동 호출 (6-7 훅).
 * 심사관 요청 시 AdvisoryRagController 가 직접 호출.
 *
 * 검색 대상:
 *   - active_yn='Y' 문서
 *   - 오늘(YYYYMMDD)이 effective_start_date ~ effective_end_date 범위에 포함된 청크
 *   - 동일 embedding_model_cd
 *
 * 결과는 ADVISORY_RETRIEVAL_LOG 에 append-only 기록 (감사용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyCitationRetriever {

    private static final int DEFAULT_TOP_K = 3;

    private final JdbcTemplate                  jdbcTemplate;
    private final EmbeddingClient               embeddingClient;
    private final AdvisoryRetrievalLogRepository logRepo;

    /**
     * 정책 인용 검색.
     *
     * @param advrId    검색을 유발한 리포트 ID (감사 로그용; null 허용)
     * @param ruleCd    트리거 룰 코드 (감사 로그용)
     * @param queryText 검색 쿼리 텍스트 (룰 신호 내용 요약)
     * @param topK      반환할 최대 청크 수
     * @param requestedBy 요청자 ID
     * @return 유사도 내림차순 정렬된 인용 목록
     */
    @Transactional
    public PolicyCitationResponse retrieve(Long advrId, String ruleCd,
                                           String queryText, int topK, Long requestedBy) {
        float[] qvec   = embeddingClient.embed(queryText);
        String  qvecStr = EmbeddingClient.toVectorString(qvec);
        String  modelCd = embeddingClient.defaultModelCd();
        String  today   = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        List<PolicyCitationResponse.CitationItem> items = new ArrayList<>();
        try {
            items = jdbcTemplate.query("""
                SELECT c.chunk_id, c.doc_id, d.doc_cd, d.doc_title, c.section_path, c.chunk_text,
                       1 - (c.embedding <=> CAST(? AS vector)) AS score
                FROM   advisory_document_chunk c
                JOIN   advisory_document d ON c.doc_id = d.doc_id
                WHERE  c.embedding_model_cd = ?
                  AND  d.active_yn = 'Y'
                  AND  d.deleted_at IS NULL
                  AND  (d.effective_start_date IS NULL OR d.effective_start_date <= ?)
                  AND  (d.effective_end_date   IS NULL OR d.effective_end_date   >= ?)
                ORDER  BY c.embedding <=> CAST(? AS vector)
                LIMIT  ?
                """,
                    (rs, i) -> new PolicyCitationResponse.CitationItem(
                            rs.getLong("chunk_id"),
                            rs.getLong("doc_id"),
                            rs.getString("doc_cd"),
                            rs.getString("doc_title"),
                            rs.getString("section_path"),
                            rs.getString("chunk_text"),
                            rs.getDouble("score")),
                    qvecStr, modelCd, today, today, qvecStr, topK);
        } catch (Exception e) {
            log.warn("정책 인용 검색 실패 (빈 결과 반환) — advrId={} rule={}: {}", advrId, ruleCd, e.getMessage());
        }

        appendLog(advrId, ruleCd, queryText, modelCd, items.size(),
                items.isEmpty() ? null : items.get(0).score(), requestedBy);

        return new PolicyCitationResponse(advrId, items.size(), items);
    }

    public PolicyCitationResponse retrieve(Long advrId, String ruleCd, String queryText, Long requestedBy) {
        return retrieve(advrId, ruleCd, queryText, DEFAULT_TOP_K, requestedBy);
    }

    // ──────────────────────────────────────────────────
    private void appendLog(Long advrId, String ruleCd, String queryText,
                           String modelCd, int resultCount, Double topScore, Long requestedBy) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            logRepo.save(AdvisoryRetrievalLog.builder()
                    .advrId(advrId)
                    .retrievalKindCd(AdvisoryRetrievalLog.KIND_POLICY_CITATION)
                    .ruleCd(ruleCd)
                    .queryText(queryText)
                    .queryEmbeddingModelCd(modelCd)
                    .resultCount(resultCount)
                    .topScore(topScore != null
                            ? BigDecimal.valueOf(topScore).setScale(6, RoundingMode.HALF_UP)
                            : null)
                    .requestedBy(requestedBy)
                    .requestedAt(now)
                    .createdAt(now)
                    .createdBy(requestedBy != null ? requestedBy : 0L)
                    .build());
        } catch (Exception e) {
            log.warn("검색 감사 로그 적재 실패 (무시) — advrId={}: {}", advrId, e.getMessage());
        }
    }
}
