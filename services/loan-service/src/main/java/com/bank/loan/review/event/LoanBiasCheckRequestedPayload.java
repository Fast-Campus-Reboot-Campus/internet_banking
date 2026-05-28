package com.bank.loan.review.event;

import java.time.OffsetDateTime;

/**
 * BIAS_CHECK_REQUESTED Kafka 이벤트 페이로드.
 * PII(주민번호·계좌번호) 는 포함하지 않는다.
 */
public record LoanBiasCheckRequestedPayload(
        String eventTypeCd,
        OffsetDateTime occurredAt,
        Long revId,
        Long applId,
        String revTypeCd,
        ReviewerDecision reviewerDecision,
        ReviewContext context
) {
    public static final String EVENT_TYPE_CD = "BIAS_CHECK_REQUESTED";

    public record ReviewerDecision(
            String decisionCd,
            String rejectReasonCd,
            Long approvedAmount,
            Integer approvedRateBps,
            Integer approvedPeriodMo,
            Long reviewerId,
            OffsetDateTime reviewedAt
    ) {}

    public record ReviewContext(
            String productCd,
            String cbDecisionCd,
            Integer cbScore,
            Integer dsrRatioBps,
            Integer dsrLimitBps,
            Integer ltvRatioBps
    ) {}
}
