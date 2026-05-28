package com.bank.loan.prescreening.client;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.prescreening.engine.CreditScoreResult;
import com.bank.loan.product.domain.LoanProduct;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * auto-loan-review POST /api/ai/auto-review/evaluate 요청 DTO.
 *
 * <p>Layer 1 개인정보(age, sex 등)는 MVP 단계에서 null 허용 — 모델이 missing 분기로 처리.
 * null 필드는 직렬화 제외.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AutoReviewEvaluateRequest(
        Long revId,
        Long annualIncomeKw,
        Long requestedAmountKw,
        Integer requestedPeriodMo,
        String purposeCd,
        String occupation,
        Integer creditScoreProxy,
        BigDecimal dsr,
        BigDecimal ltv,
        String productCode,
        Integer age,
        String sex,
        String maritalStatus,
        String educationLevel,
        String housingType
) {

    public static AutoReviewEvaluateRequest of(LoanApplication app,
                                                CreditScoreResult engineResult,
                                                LoanProduct product) {
        return new AutoReviewEvaluateRequest(
                null,
                app.getEstimatedIncomeAmt(),
                app.getRequestedAmount(),
                app.getRequestedPeriodMo(),
                app.getLoanPurposeCd(),
                app.getEmploymentTypeCd(),
                engineResult != null ? engineResult.score() : null,
                null,
                null,
                product != null ? product.getProdCd() : null,
                null, null, null, null, null
        );
    }
}
