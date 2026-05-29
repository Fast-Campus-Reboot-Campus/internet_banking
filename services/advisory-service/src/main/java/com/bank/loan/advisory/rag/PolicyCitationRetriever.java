package com.bank.loan.advisory.rag;

import com.bank.loan.advisory.domain.AdvisoryRetrievalLog;
import com.bank.loan.advisory.dto.PolicyCitationResponse;
import com.bank.loan.advisory.repository.AdvisoryRetrievalLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 정책문서 청크 코사인 유사도 검색 (시나리오 δ — ai-service 어댑터).
 *
 * 기존: advisory_document_chunk JDBC 직접 쿼리
 * 변경: ai-service POST /rag/search (profile=review) 위임
 *
 * CRITICAL 룰 발화 시 AdvisoryEvaluator 가 자동 호출 (6-7 훅).
 * 심사관 요청 시 AdvisoryRagController 가 직접 호출.
 * 결과는 ADVISORY_RETRIEVAL_LOG 에 append-only 기록 (감사용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyCitationRetriever {

    private static final int    DEFAULT_TOP_K   = 3;
    private static final String PROFILE_REVIEW  = "review";

    private final AiServiceRagClient             aiServiceRagClient;
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
        List<AiServiceRagClient.ChunkHit> hits = aiServiceRagClient.search(queryText, PROFILE_REVIEW, topK);

        List<PolicyCitationResponse.CitationItem> items = hits.stream()
                .map(h -> new PolicyCitationResponse.CitationItem(
                        h.chunkId(),
                        h.docId(),
                        h.title(),   // ai-service 는 docCd 미노출 → title 대체
                        h.title(),
                        h.sourceUri(),
                        h.content(),
                        h.score()))
                .toList();

        appendLog(advrId, ruleCd, queryText, "ai-service", items.size(),
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
