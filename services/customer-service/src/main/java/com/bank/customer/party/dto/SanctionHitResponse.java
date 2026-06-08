package com.bank.customer.party.dto;

import java.time.OffsetDateTime;

/**
 * 제재 스크리닝 Hit 검토 행 — 제재대상 Hit 검토 화면(/admin/screening)의 검토 큐.
 *
 * <p>sanction_screening_hit를 party 이름·인적사항과 함께 반환한다(일치율·Hit유형·검토상태 포함).
 */
public record SanctionHitResponse(
        Long           sanctionScreeningHitId,
        Long           partyId,
        String         partyName,
        String         birthDate,
        String         nationalityCode,
        String         hitTypeCode,
        Integer        matchRate,
        String         screeningStatusCode,
        OffsetDateTime detectedAt
) {
}
