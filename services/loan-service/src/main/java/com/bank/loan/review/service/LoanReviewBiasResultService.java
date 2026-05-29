package com.bank.loan.review.service;

import com.bank.loan.review.domain.AiReviewAdvice;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.BiasResultCallbackRequest;
import com.bank.loan.review.repository.AiReviewAdviceRepository;
import com.bank.loan.review.repository.LoanReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * review-ai-gateway 편향 검증 결과 콜백 처리.
 * BIAS_REVIEWING 상태인 경우에만 severity 갱신 — 중복 수신은 무시(멱등).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanReviewBiasResultService {

    private final LoanReviewRepository reviewRepository;
    private final AiReviewAdviceRepository adviceRepository;

    @Transactional
    public void apply(Long revId, BiasResultCallbackRequest req) {
        LoanReview review = reviewRepository.findById(revId).orElse(null);
        if (review == null) {
            log.warn("bias-result 콜백 수신했으나 심사 건 없음 revId={}", revId);
            return;
        }

        String severityCd = toSeverity(req.status());

        String body = req.findingSummary() != null ? req.findingSummary() : req.status();
        adviceRepository.save(AiReviewAdvice.builder()
                .revId(revId)
                .adviceTypeCd(AiReviewAdvice.TYPE_BIAS_CHECK)
                .severityCd(severityCd)
                .adviceBody(body)
                .build());

        if (review.isBiasReviewing()) {
            review.updateBiasSeverity(severityCd);
            log.info("bias-result 적용 revId={} severity={}", revId, severityCd);
        } else {
            log.warn("bias-result 수신했으나 BIAS_REVIEWING 상태 아님 revId={} status={}",
                    revId, review.getRevStatusCd());
        }
    }

    private static String toSeverity(String status) {
        return switch (status) {
            case BiasResultCallbackRequest.STATUS_FLAGGED -> AiReviewAdvice.SEVERITY_BLOCKED;
            case BiasResultCallbackRequest.STATUS_PASS   -> AiReviewAdvice.SEVERITY_NONE;
            default                                      -> AiReviewAdvice.SEVERITY_LOW;
        };
    }
}
