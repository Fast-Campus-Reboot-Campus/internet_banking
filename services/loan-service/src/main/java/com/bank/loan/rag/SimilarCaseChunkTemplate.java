package com.bank.loan.rag;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.review.domain.LoanReview;

/**
 * 유사 케이스 청크 텍스트 생성기 — D3-2.
 *
 * <p>개인식별정보(PII) 를 포함하지 않는 구조적 텍스트로 변환.
 * 고용 유형, 소득 분위(버킷), 신청 금액 범위, 기간, 목적 코드, 결정 코드만 포함.
 */
public final class SimilarCaseChunkTemplate {

    private SimilarCaseChunkTemplate() {}

    /**
     * loan_review + loan_application 을 조합해 임베딩용 청크 텍스트를 생성.
     *
     * <p>형식 (예시):
     * <pre>
     * [유사심사] 고용=직장인 소득분위=Q4 신청=1억5천~2억/360개월/아파트구입 결정=APPROVE 유형=AUTO
     * </pre>
     */
    public static String build(LoanReview review, LoanApplication application) {
        String employment = nullSafe(application.getEmploymentTypeCd(), "미상");
        String incomeQ    = incomeQuintile(application.getEstimatedIncomeAmt());
        String amount     = amountRange(application.getRequestedAmount());
        String period     = application.getRequestedPeriodMo() + "개월";
        String purpose    = nullSafe(application.getLoanPurposeCd(), "미상");
        String decision   = nullSafe(review.getRevDecisionCd(), "미결");
        String revType    = nullSafe(review.getRevTypeCd(), "미상");

        return "[유사심사] 고용=%s 소득분위=%s 신청=%s/%s/%s 결정=%s 유형=%s"
                .formatted(employment, incomeQ, amount, period, purpose, decision, revType);
    }

    /** 임베딩 코퍼스 식별자. */
    public static String corpus() {
        return "similar_cases";
    }

    /** ai_embedding.source_id — revId 를 문자열로 사용. */
    public static String sourceId(Long revId) {
        return "rev-" + revId;
    }

    // ─────────────────────────────────────────────────────────────────────

    static String incomeQuintile(Long incomeAmt) {
        if (incomeAmt == null) return "Q?";
        long amt = incomeAmt;
        if (amt < 30_000_000L)  return "Q1";
        if (amt < 50_000_000L)  return "Q2";
        if (amt < 80_000_000L)  return "Q3";
        if (amt < 120_000_000L) return "Q4";
        return "Q5";
    }

    static String amountRange(Long amount) {
        if (amount == null) return "미상";
        long amt = amount;
        if (amt < 50_000_000L)   return "5천만미만";
        if (amt < 100_000_000L)  return "5천~1억";
        if (amt < 200_000_000L)  return "1억~2억";
        if (amt < 300_000_000L)  return "2억~3억";
        if (amt < 500_000_000L)  return "3억~5억";
        return "5억이상";
    }

    private static String nullSafe(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
