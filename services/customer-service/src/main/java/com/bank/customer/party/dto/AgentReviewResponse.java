package com.bank.customer.party.dto;

/**
 * 대리인 위임장 검토 대기 행 — 대리인 검토 화면(/admin/agent)의 진입점.
 *
 * <p>review_status='PENDING'인 party_relation을 대리인(toParty) 이름과 함께 반환한다.
 * 규약: fromPartyId=본인(위임자), toPartyId=대리인(수임자). agentName은 대리인(toParty)의 이름.
 * 승인/거절은 PATCH로 처리한다.
 */
public record AgentReviewResponse(
        Long   relationId,
        Long   ownerPartyId,
        Long   agentPartyId,
        String agentName,
        String relationTypeCode,
        String relationDetailCode,
        String representationScope,
        String proofUrl,
        String relationStartDate,
        String relationReviewStatusCode
) {
}
