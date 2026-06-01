package com.bank.loan.advisory.rag;

import com.bank.common.web.BusinessException;
import com.bank.loan.advisory.domain.AdvisoryCaseIndex;
import com.bank.loan.advisory.domain.AdvisoryRetrievalLog;
import com.bank.loan.advisory.domain.ReviewAdvisoryReport;
import com.bank.loan.advisory.dto.SimilarCaseResponse;
import com.bank.loan.advisory.repository.AdvisoryCaseIndexRepository;
import com.bank.loan.advisory.repository.AdvisoryRetrievalLogRepository;
import com.bank.loan.advisory.repository.ReviewAdvisoryReportRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 유사 과거 심사 사례 코사인 유사도 검색 (시나리오 δ — ai-service 어댑터).
 *
 * 기존: advisory_case_index JDBC 직접 쿼리 (EmbeddingClient + pgvector)
 * 변경: ai-service POST /rag/search (profile=similar-case) 위임
 *       → 반환된 chunkId(=case_idx_id)로 advisory DB에서 메타데이터 보완
 *
 * revId→LoanReview→텍스트 변환은 advisory DB에 종속되므로 advisory 내부 유지.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarCaseRetriever {

    private static final int DEFAULT_TOP_K = 5;

    private final AiServiceRagClient              aiServiceRagClient;
    private final AdvisoryCaseIndexRepository     caseIndexRepo;
    private final ReviewAdvisoryReportRepository  reportRepo;
    private final LoanReviewRepository            reviewRepo;
    private final AdvisoryRetrievalLogRepository  logRepo;

    @Transactional
    public SimilarCaseResponse retrieve(Long advrId, Long actorId) {
        return retrieve(advrId, DEFAULT_TOP_K, actorId);
    }

    @Transactional
    public SimilarCaseResponse retrieve(Long advrId, int topK, Long actorId) {
        ReviewAdvisoryReport report = reportRepo.findById(advrId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_190));

        LoanReview review = reviewRepo.findById(report.getRevId())
                .filter(r -> r.getDeletedAt() == null)
                .orElse(null);

        String queryText = buildQueryText(report, review);

        List<AiServiceRagClient.ChunkHit> hits = aiServiceRagClient.search(queryText, "similar-case", topK);

        List<Long> caseIdxIds = hits.stream().map(AiServiceRagClient.ChunkHit::chunkId).toList();
        Map<Long, AdvisoryCaseIndex> caseMap = caseIndexRepo.findAllById(caseIdxIds).stream()
                .collect(Collectors.toMap(AdvisoryCaseIndex::getCaseIdxId, c -> c));

        // ai-service 점수 순서 유지, 자기 자신(revId 일치) 제외
        List<SimilarCaseResponse.CaseItem> items = hits.stream()
                .map(h -> {
                    AdvisoryCaseIndex c = caseMap.get(h.chunkId());
                    if (c == null || c.getRevId().equals(report.getRevId())) return null;
                    return new SimilarCaseResponse.CaseItem(
                            c.getCaseIdxId(),
                            c.getRevId(),
                            c.getDecisionCd(),
                            c.getOverturnYn(),
                            c.getCreditScore(),
                            c.getDsrRatioBps(),
                            c.getLtvRatioBps(),
                            c.getCohortEmploymentTypeCd(),
                            c.getCohortLoanPurposeCd(),
                            c.getSummaryText(),
                            h.score());
                })
                .filter(Objects::nonNull)
                .toList();

        appendLog(advrId, queryText, "ai-service", items.size(),
                items.isEmpty() ? null : items.get(0).score(), actorId);

        return new SimilarCaseResponse(advrId, items.size(), items);
    }

    // ──────────────────────────────────────────────────
    private String buildQueryText(ReviewAdvisoryReport report, LoanReview review) {
        if (review == null) {
            return String.format("룰코드: %s 심각도: %s", report.getRuleId(), report.getSeverityCd());
        }
        return String.format("심사결정: %s 심사유형: %s 심사관ID: %s 룰코드: %s",
                review.getRevDecisionCd(), review.getRevTypeCd(),
                review.getReviewerId(), report.getRuleId());
    }

    private void appendLog(Long advrId, String queryText, String modelCd,
                           int resultCount, Double topScore, Long actorId) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            logRepo.save(AdvisoryRetrievalLog.builder()
                    .advrId(advrId)
                    .retrievalKindCd(AdvisoryRetrievalLog.KIND_SIMILAR_CASE)
                    .queryText(queryText)
                    .queryEmbeddingModelCd(modelCd)
                    .resultCount(resultCount)
                    .topScore(topScore != null
                            ? BigDecimal.valueOf(topScore).setScale(6, RoundingMode.HALF_UP)
                            : null)
                    .requestedBy(actorId)
                    .requestedAt(now)
                    .createdAt(now)
                    .createdBy(actorId != null ? actorId : 0L)
                    .build());
        } catch (Exception e) {
            log.warn("검색 감사 로그 적재 실패 (무시) — advrId={}: {}", advrId, e.getMessage());
        }
    }
}
