package com.bank.loan.guaranteeinsuranceexpiry.dto;

/**
 * 보증보험 만기 일배치 결과.
 *
 *   baseDate          기준일 YYYYMMDD — 이 날짜보다 만기일(gins_end_date) 이 이른 ISSUED 가 대상
 *   totalCandidates   조회된 만료 후보 수
 *   processed         실제 EXPIRED 로 전이된 건수
 */
public record GuaranteeInsuranceExpiryRunResponse(
        String baseDate,
        int totalCandidates,
        int processed
) {
    public static GuaranteeInsuranceExpiryRunResponse of(String baseDate, int total, int processed) {
        return new GuaranteeInsuranceExpiryRunResponse(baseDate, total, processed);
    }
}
