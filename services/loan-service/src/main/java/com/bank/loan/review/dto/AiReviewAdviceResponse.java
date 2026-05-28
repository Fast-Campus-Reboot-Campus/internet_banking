package com.bank.loan.review.dto;

import com.bank.loan.review.domain.AiReviewAdvice;

import java.time.OffsetDateTime;

public record AiReviewAdviceResponse(
        Long adviceId,
        Long revId,
        String adviceTypeCd,
        String severityCd,
        String adviceBody,
        String model,
        String modelVersion,
        Integer latencyMs,
        OffsetDateTime createdAt
) {
    public static AiReviewAdviceResponse of(AiReviewAdvice a) {
        return new AiReviewAdviceResponse(
                a.getAdviceId(), a.getRevId(),
                a.getAdviceTypeCd(), a.getSeverityCd(), a.getAdviceBody(),
                a.getModel(), a.getModelVersion(), a.getLatencyMs(), a.getCreatedAt()
        );
    }
}
