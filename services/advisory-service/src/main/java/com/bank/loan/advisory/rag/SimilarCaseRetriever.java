package com.bank.loan.advisory.rag;

import com.bank.common.web.BusinessException;
import com.bank.loan.advisory.domain.AdvisoryRetrievalLog;
import com.bank.loan.advisory.domain.ReviewAdvisoryReport;
import com.bank.loan.advisory.dto.SimilarCaseResponse;
import com.bank.loan.advisory.repository.AdvisoryRetrievalLogRepository;
import com.bank.loan.advisory.repository.ReviewAdvisoryReportRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 유사 과거 심사 사례 코사인 유사도 검색 (plan §11.4.3 — Task 6-5).
 *
 * 요청 시나리오: 심사관이 GET /advisory/reports/{advr_id}/similar-cases 호출.
 *   1. 리포트 → revId 로드
 *   2. revId → LoanReview 로드 → 구조화 쿼리 텍스트 생성
 *   3. embed → ADVISORY_CASE_INDEX 코사인 유사도 검색
 *   4. ADVISORY_RETRIEVAL_LOG append
 *
 * OVERTURN 부스트: 결과 top-K 중 overturn_yn='Y' 사례를 앞으로 정렬
 * (점수가 같을 때만 우선 — cosine 점수 순 유지 원칙 준수, plan §11.7 §3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarCaseRetriever {

    private static final int DEFAULT_TOP_K = 5;

    private final JdbcTemplate                   jdbcTemplate;
    private final EmbeddingClient                embeddingClient;
    private final ReviewAdvisoryReportRepository reportRepo;
    private final LoanReviewRepository           reviewRepo;
    private final AdvisoryRetrievalLogRepository logRepo;

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
        String modelCd  = embeddingClient.defaultModelCd();
        float[] qvec    = embeddingClient.embed(queryText);
        String  qvecStr = EmbeddingClient.toVectorString(qvec);

        List<SimilarCaseResponse.CaseItem> items = new ArrayList<>();
        try {
            items = jdbcTemplate.query("""
                SELECT ci.case_idx_id, ci.rev_id, ci.decision_cd, ci.overturn_yn,
                       ci.credit_score, ci.dsr_ratio_bps, ci.ltv_ratio_bps,
                       ci.cohort_employment_type_cd, ci.cohort_loan_purpose_cd,
                       ci.summary_text,
                       1 - (ci.embedding <=> CAST(? AS vector)) AS score
                FROM   advisory_case_index ci
                WHERE  ci.embedding_model_cd = ?
                  AND  ci.rev_id <> ?
                ORDER  BY ci.embedding <=> CAST(? AS vector)
                LIMIT  ?
                """,
                    (rs, i) -> new SimilarCaseResponse.CaseItem(
                            rs.getLong("case_idx_id"),
                            rs.getLong("rev_id"),
                            rs.getString("decision_cd"),
                            rs.getString("overturn_yn"),
                            (Integer) rs.getObject("credit_score"),
                            (Integer) rs.getObject("dsr_ratio_bps"),
                            (Integer) rs.getObject("ltv_ratio_bps"),
                            rs.getString("cohort_employment_type_cd"),
                            rs.getString("cohort_loan_purpose_cd"),
                            rs.getString("summary_text"),
                            rs.getDouble("score")),
                    qvecStr, modelCd, report.getRevId(), qvecStr, topK);
        } catch (Exception e) {
            log.warn("유사 사례 검색 실패 (빈 결과 반환) — advrId={}: {}", advrId, e.getMessage());
        }

        appendLog(advrId, queryText, modelCd, items.size(),
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
