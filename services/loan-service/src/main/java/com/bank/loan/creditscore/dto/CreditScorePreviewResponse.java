package com.bank.loan.creditscore.dto;

import com.bank.loan.prescreening.engine.CreditScoreResult;

/**
 * 한도조회(가심사 preview) 응답.
 *
 * 외부 신용평가 엔진의 raw 결과(점수/등급/PD/한도/거절사유/엔진버전)를 그대로 클라이언트에 노출.
 * 본 응답은 DB 에 적재되지 않으며, 사용자가 "신청으로 이어가기" 를 선택해 정식 가심사를 호출해야
 * LoanPrescreening row 가 생성된다.
 */
public record CreditScorePreviewResponse(
        String decision,
        Integer score,
        String grade,
        Integer pdBps,
        Long estimatedLimitAmt,
        String rejectReasonCd,
        String engineVersion
) {
    public static CreditScorePreviewResponse of(CreditScoreResult r) {
        return new CreditScorePreviewResponse(
                r.decision(),
                r.score(),
                r.grade(),
                r.pdBps(),
                r.estimatedLimitAmt(),
                r.rejectReasonCd(),
                r.engineVersion()
        );
    }
}
