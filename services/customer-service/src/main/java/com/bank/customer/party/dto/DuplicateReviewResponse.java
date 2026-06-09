package com.bank.customer.party.dto;

import java.time.OffsetDateTime;

/**
 * 중복고객 검토 행 — 중복고객 검토 화면(/admin/duplicates)의 검토 큐.
 *
 * <p>duplicate_review_case를 신규·기존 party 이름과 함께 반환한다.
 */
public record DuplicateReviewResponse(
        Long           duplicateReviewCaseId,
        Long           newPartyId,
        String         newPartyName,
        Long           existingPartyId,
        String         existingPartyName,
        String         matchTypeCode,
        String         reviewStatusCode,
        OffsetDateTime detectedAt
) {
}
