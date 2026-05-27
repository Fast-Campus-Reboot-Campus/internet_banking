package com.bank.ai.review.dto;

import java.util.Map;

/**
 * 자동심사 결과. decision 은 {@code APPROVE/CONDITIONAL/REJECT}.
 * score 는 결정된 클래스의 확률(0~1). proba 는 전 클래스 확률 분포.
 */
public record AutoReviewResponse(
        String modelVersion,
        String decision,
        double score,
        Map<String, Double> proba
) {
}
