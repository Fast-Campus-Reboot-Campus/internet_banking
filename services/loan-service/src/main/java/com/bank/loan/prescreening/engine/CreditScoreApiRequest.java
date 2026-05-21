package com.bank.loan.prescreening.engine;

/**
 * 외부 신용평가사 HTTP API 요청 페이로드. 우리 {@link CreditScoreRequest} 를 외부 스펙으로 매핑.
 *
 * 외부 API 스펙(가정) — 실제 벤더(KCB/NICE 등) 시그니처와 다를 수 있어 운영 도입 시 매핑만 갱신.
 */
public record CreditScoreApiRequest(
        Long customerId,
        String loanType,
        Long amount,
        Integer period,
        String purpose,
        String employmentType,
        Long income
) {
    public static CreditScoreApiRequest of(CreditScoreRequest req) {
        return new CreditScoreApiRequest(
                req.customerId(),
                req.loanTypeCd(),
                req.requestedAmount(),
                req.requestedPeriodMo(),
                req.loanPurposeCd(),
                req.employmentTypeCd(),
                req.estimatedIncomeAmt()
        );
    }
}
