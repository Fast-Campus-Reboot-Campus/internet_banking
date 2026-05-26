package com.bank.loan.maturity.dto;

/**
 * 만기 도래 일배치 결과.
 *
 *   baseDate          기준일 YYYYMMDD — 이 날짜까지 만기일이 도달한 ACTIVE 가 대상
 *   totalCandidates   조회된 만기 도래 후보 수
 *   processed         실제 MATURED 로 전이된 건수
 */
public record MaturityRunResponse(
        String baseDate,
        int totalCandidates,
        int processed
) {
    public static MaturityRunResponse of(String baseDate, int total, int processed) {
        return new MaturityRunResponse(baseDate, total, processed);
    }
}
