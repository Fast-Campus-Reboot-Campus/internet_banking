package com.bank.loan.rag.outbox;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.rag.SimilarCaseChunkTemplate;
import com.bank.loan.review.domain.LoanReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 유사 케이스 outbox 발행기 — Phase E (E3-4).
 *
 * <p>결정 완료된 LOAN_REVIEW 를 PII-free 청크 페이로드로 변환해 {@code loan_review_outbox} 에 적재.
 * 호출자 트랜잭션에 참여({@link Propagation#REQUIRED})하므로 심사 결정 저장과 원자적으로 commit 된다.
 * 멱등 키({@code CASE_INDEXED:revId})로 동일 케이스 중복 적재를 차단.
 *
 * <p>페이로드는 {@code /api/internal/embeddings/batch} 의 청크 스키마(corpus/source_id/chunk_seq/
 * chunk_text/metadata)와 동일 — 다운스트림(enricher/Connect)이 그대로 색인할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarCaseOutboxPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LoanReviewOutboxRepository repository;

    /**
     * 호출자 트랜잭션에 참여해 케이스 청크를 outbox 에 적재.
     *
     * @param review      완료된 심사
     * @param application 신청 (청크 텍스트 구성용)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueueInCurrentTx(LoanReview review, LoanApplication application) {
        String key = LoanReviewOutbox.idempotencyKeyOf(
                LoanReviewOutbox.EVENT_TYPE_CASE_INDEXED, review.getRevId());
        if (repository.existsByIdempotencyKey(key)) {
            log.debug("[case-outbox] skip duplicate key={}", key);
            return;
        }

        String payload = buildPayload(review, application);
        try {
            repository.save(LoanReviewOutbox.caseIndexed(review.getRevId(), payload));
            log.debug("[case-outbox] enqueued revId={}", review.getRevId());
        } catch (DataIntegrityViolationException race) {
            log.debug("[case-outbox] race on key={}", key);
        }
    }

    private static String buildPayload(LoanReview review, LoanApplication application) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("rev_type", review.getRevTypeCd());
        metadata.put("decision", String.valueOf(review.getRevDecisionCd()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("corpus", SimilarCaseChunkTemplate.corpus());
        payload.put("source_id", SimilarCaseChunkTemplate.sourceId(review.getRevId()));
        payload.put("chunk_seq", 0);
        payload.put("chunk_text", SimilarCaseChunkTemplate.build(review, application));
        payload.put("metadata", metadata);

        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("case outbox payload 직렬화 실패 revId=" + review.getRevId(), e);
        }
    }
}
