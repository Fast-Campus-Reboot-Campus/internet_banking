package com.bank.loan.review.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.review.domain.AiReviewAdvice;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.AiReviewAdviceResponse;
import com.bank.loan.review.dto.BiasReportRequest;
import com.bank.loan.review.repository.AiReviewAdviceRepository;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanReviewBiasReportService {

    private final LoanReviewRepository reviewRepository;
    private final AiReviewAdviceRepository adviceRepository;
    private final ObjectMapper objectMapper;

    /**
     * 편향 에이전트가 분석 결과를 밀어넣는 내부 API 핸들러.
     * BIAS_REVIEWING 상태가 아니면 207(already-processed) 처리 — 재전송 무시.
     * advice_body 에는 summary + findings JSON 전체를 저장.
     */
    @Transactional
    public AiReviewAdviceResponse append(Long revId, BiasReportRequest req) {
        LoanReview review = reviewRepository.findById(revId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        String body = buildAdviceBody(req);

        AiReviewAdvice advice = adviceRepository.save(AiReviewAdvice.builder()
                .revId(revId)
                .adviceTypeCd(AiReviewAdvice.TYPE_BIAS_CHECK)
                .severityCd(req.severityCd())
                .adviceBody(body)
                .model(req.model())
                .modelVersion(req.modelVersion())
                .promptHash(req.promptHash())
                .inputToken(req.inputToken())
                .outputToken(req.outputToken())
                .latencyMs(req.latencyMs())
                .build());

        // BIAS_REVIEWING 상태일 때만 severity 캐시 갱신 (이미 진행된 건은 덮어쓰지 않음)
        if (review.isBiasReviewing()) {
            review.updateBiasSeverity(req.severityCd());
        }

        return AiReviewAdviceResponse.of(advice);
    }

    @Transactional(readOnly = true)
    public List<AiReviewAdviceResponse> listByRev(Long revId) {
        reviewRepository.findById(revId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));
        return adviceRepository.findByRevIdOrderByCreatedAtDesc(revId)
                .stream()
                .map(AiReviewAdviceResponse::of)
                .toList();
    }

    private String buildAdviceBody(BiasReportRequest req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("bias-report body 직렬화 실패", e);
        }
    }
}
