package com.bank.ai.review.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 자동심사 신청 입력.
 *
 * 학습 모델의 29개 피처와 동일한 키를 사용한다. 입력 누락 시 모델이 missing 분기로 라우팅.
 * BigDecimal 이 아닌 primitive 류를 쓰는 이유: 금액 비교가 아니라 ML 모델 피처라 정밀도 요구 없음.
 */
public record AutoReviewRequest(
        // ---- Layer 1: persona ----
        String sex,
        @NotNull @Min(0) Integer age,
        String maritalStatus,
        String militaryStatus,
        String familyType,
        String housingType,
        String educationLevel,
        String bachelorsField,
        String occupation,
        String district,
        String province,
        String applicantSegment,

        // ---- Layer 2: financial ----
        Integer incomeQuintile,
        Long annualIncomeKw,
        Long totalAssetKw,
        Long totalDebtKw,
        Long collateralDebtKw,
        Long creditDebtKw,
        Double dsr,
        Double ltv,
        Long monthlyCashflowMeanKw,
        Long monthlyCashflowStdKw,
        Integer delinquencyHistory24m,
        Integer creditScoreProxy,

        // ---- Layer 3: application ----
        String productCode,
        Long requestedAmountKw,
        Integer requestedPeriodMo,
        String purposeCd,
        Boolean purposeRedFlag
) {
}
