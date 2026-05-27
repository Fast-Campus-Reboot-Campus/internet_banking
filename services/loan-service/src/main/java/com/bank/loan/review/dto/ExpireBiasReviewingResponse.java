package com.bank.loan.review.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ExpireBiasReviewingResponse(
        int processed,
        List<Long> expiredRevIds,
        OffsetDateTime cutoffAt
) {}
