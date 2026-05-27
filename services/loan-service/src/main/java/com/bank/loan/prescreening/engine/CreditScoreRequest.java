package com.bank.loan.prescreening.engine;

/**
 * 외부 신용평가(가심사) 엔진 호출 입력.
 *
 * 가심사 단계에서 외부 엔진(예: KCB/NICE 또는 사내 PD 모델)에 전달되는 신청자·신청 정보.
 * 실제 운영에선 추가 식별 정보(주민번호 hash 등)가 더 필요할 수 있으나 MVP 범위에선 신청 기반 필드만 다룬다.
 *
 *   customerId           — 신청 고객 식별
 *   loanTypeCd           — 상품 유형 (CREDIT/MORTGAGE) — 한도/룰 분기에 사용
 *   requestedAmount      — 신청 금액 (원)
 *   requestedPeriodMo    — 신청 기간 (개월)
 *   loanPurposeCd        — 신청 목적 (LIVING/HOUSE/...)
 *   employmentTypeCd     — 고용 형태 (EMPLOYEE/SELF_EMPLOYED/...) 자가신고
 *   estimatedIncomeAmt   — 추정 연소득 (원) 자가신고
 */
public record CreditScoreRequest(
        Long customerId,
        String loanTypeCd,
        Long requestedAmount,
        Integer requestedPeriodMo,
        String loanPurposeCd,
        String employmentTypeCd,
        Long estimatedIncomeAmt
) {
}
