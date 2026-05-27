package com.bank.loan.review.service;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.product.domain.LoanProduct;
import org.springframework.stereotype.Component;

/**
 * 본심사 승인 한도 산정 — 보수적 정책: min(신청금액, CB 한도, 상품 최대).
 * 입력 source 변경 시 본 클래스만 수정.
 */
@Component
public class ApprovedAmountCalculator {

    public long determine(LoanApplication application, CreditEvaluation ceval, LoanProduct product) {
        long candidate = application.getRequestedAmount();
        if (ceval != null && ceval.getEvalLimitAmount() != null) {
            candidate = Math.min(candidate, ceval.getEvalLimitAmount());
        }
        if (product != null && product.getMaxAmount() != null) {
            candidate = Math.min(candidate, product.getMaxAmount());
        }
        return candidate;
    }
}
