package com.bank.loan.rag;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.review.domain.LoanReview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SimilarCaseChunkTemplate 단위 테스트 — D3-2.
 * PII-free 텍스트 생성 및 소득 분위·금액 범위 버킷 검증.
 */
class SimilarCaseChunkTemplateTest {

    @Test
    void 정상_케이스_청크_텍스트_포맷_검증() {
        var review = mockReview(1L, "APPROVE", "AUTO");
        var app = mockApp("직장인", 60_000_000L, 150_000_000L, 360, "아파트구입");

        String text = SimilarCaseChunkTemplate.build(review, app);

        assertThat(text).contains("[유사심사]");
        assertThat(text).contains("고용=직장인");
        assertThat(text).contains("소득분위=Q3"); // 60M → Q3 (50M~80M)
        assertThat(text).contains("신청=1억~2억/360개월/아파트구입"); // 150M → 1억~2억
        assertThat(text).contains("결정=APPROVE");
        assertThat(text).contains("유형=AUTO");
    }

    @ParameterizedTest
    @CsvSource({
            "10000000,  Q1",
            "30000000,  Q2",
            "50000000,  Q3",
            "80000000,  Q4",
            "120000000, Q5"
    })
    void 소득분위_버킷_경계값(long income, String expected) {
        assertThat(SimilarCaseChunkTemplate.incomeQuintile(income)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "10000000,  5천만미만",
            "50000000,  5천~1억",
            "100000000, 1억~2억",
            "200000000, 2억~3억",
            "300000000, 3억~5억",
            "500000000, 5억이상"
    })
    void 금액범위_버킷_경계값(long amount, String expected) {
        assertThat(SimilarCaseChunkTemplate.amountRange(amount)).isEqualTo(expected);
    }

    @Test
    void null_필드는_미상으로_대체() {
        var review = mockReview(2L, null, null);
        var app = mockApp(null, null, null, 120, null);

        String text = SimilarCaseChunkTemplate.build(review, app);

        assertThat(text).contains("고용=미상");
        assertThat(text).contains("소득분위=Q?");
        assertThat(text).contains("결정=미결");
    }

    @Test
    void sourceId_형식_검증() {
        assertThat(SimilarCaseChunkTemplate.sourceId(123L)).isEqualTo("rev-123");
    }

    // ─────────────────────────────────────────────────────────────────────

    private static LoanReview mockReview(Long revId, String decision, String revType) {
        var r = mock(LoanReview.class);
        when(r.getRevId()).thenReturn(revId);
        when(r.getRevDecisionCd()).thenReturn(decision);
        when(r.getRevTypeCd()).thenReturn(revType);
        return r;
    }

    private static LoanApplication mockApp(String employment, Long income,
                                           Long amount, int period, String purpose) {
        var a = mock(LoanApplication.class);
        when(a.getEmploymentTypeCd()).thenReturn(employment);
        when(a.getEstimatedIncomeAmt()).thenReturn(income);
        when(a.getRequestedAmount()).thenReturn(amount);
        when(a.getRequestedPeriodMo()).thenReturn(period);
        when(a.getLoanPurposeCd()).thenReturn(purpose);
        return a;
    }
}
