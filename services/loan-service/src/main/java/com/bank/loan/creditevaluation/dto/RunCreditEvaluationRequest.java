package com.bank.loan.creditevaluation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 신용평가(CB) 실행 요청. 외부 CB·자동심사 엔진 stub — 결과를 클라이언트가 입력으로 결정.
 *
 * cevalEngine 예: "KCB", "NICE", "INTERNAL_XGB" 등 (engine 식별자).
 * cevalDecisionCd: APPROVE | REVIEW | REJECT
 *
 * cevalFactors: 모델 입력 피처/SHAP 값 등 JSON 문자열 — 그대로 JSONB에 저장.
 */
public record RunCreditEvaluationRequest(

        @NotBlank @Size(max = 50) String cevalEngine,
        @Size(max = 50) String cevalEngineVersion,

        @Size(max = 10) String cevalGrade,
        @Min(0) Integer cevalScore,
        @Min(0) Integer pdBps,

        @NotBlank @Pattern(regexp = "APPROVE|REVIEW|REJECT") String cevalDecisionCd,

        @Min(0) Long evalLimitAmount,
        @Min(0) Integer evalRateBps,

        String cevalFactors
) {
}
