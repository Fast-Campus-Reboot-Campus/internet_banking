package com.bank.customer.party.dto;

/**
 * KYC 만료 예정 행 — KYC 재이행(갱신) 모니터링용.
 *
 * <p>kyc_status='COMPLETED'이면서 kyc_expiry_date가 기준일 이하인 party를 이름과 함께 반환한다.
 * 기존 findKycExpiringBefore() 쿼리를 페이지네이션·이름조인으로 확장해 노출한다.
 */
public record KycExpiringResponse(
        Long   partyId,
        String partyName,
        String kycStatusCode,
        String kycExpiryDate,
        String kycNextReviewDate
) {
}
