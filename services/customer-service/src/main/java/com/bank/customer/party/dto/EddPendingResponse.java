package com.bank.customer.party.dto;

/**
 * EDD(강화된 고객확인) 심사 대기 행 — EDD 심사·승인 화면(/admin/edd)의 진입점.
 *
 * <p>compliance_info에서 {@code edd_required_yn='T'}인 party를 이름과 함께 반환한다.
 * 자금원천·실소유자·제출서류 등 심사 상세는 단건 컴플라이언스 조회/별 도메인에서 가져온다.
 * 식별자는 partyId — 모든 컴플라이언스 액션(AML/KYC PATCH)이 partyId 기준이라 일관된다.
 */
public record EddPendingResponse(
        Long   partyId,
        String partyName,
        String amlRiskLevelCode,
        String cddLevelCode,
        String kycStatusCode,
        String eddNextReviewDate
) {
}
