package com.bank.loan.rag;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.repository.LoanReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SimilarCaseExporter 단위 테스트 — D3-2.
 * 메모리 `feedback_test_isolation_dates`: 배치 테스트는 테스트별 다른 연도 사용.
 */
@ExtendWith(MockitoExtension.class)
class SimilarCaseExporterTest {

    @Mock LoanReviewRepository reviewRepo;
    @Mock LoanApplicationRepository applicationRepo;
    @Mock AutoReviewAiClient aiClient;

    private SimilarCaseExporter exporter;

    private static final SimilarCaseExportProperties PROPS = new SimilarCaseExportProperties(
            true, "http://localhost:8086", "test-token", 200, 1);

    @BeforeEach
    void setUp() {
        exporter = new SimilarCaseExporter(reviewRepo, applicationRepo, aiClient, PROPS);
    }

    @Test
    void 정상_2건_내보내기() {
        var review1 = reviewStub(1L, 101L, "APPROVE", "AUTO");
        var review2 = reviewStub(2L, 102L, "REJECT", "AUTO");
        var app1 = appStub(101L, "직장인", 90_000_000L, 200_000_000L, 360, "아파트구입");
        var app2 = appStub(102L, "자영업", 40_000_000L, 50_000_000L, 120, "운전자금");

        // 날짜 격리: 2030년 사용
        OffsetDateTime since = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        when(reviewRepo.findExportable(since)).thenReturn(List.of(review1, review2));
        when(applicationRepo.findById(101L)).thenReturn(Optional.of(app1));
        when(applicationRepo.findById(102L)).thenReturn(Optional.of(app2));
        when(aiClient.sendBatch(any())).thenReturn(2);

        int result = exporter.export(since);

        assertThat(result).isEqualTo(2);
        var captor = ArgumentCaptor.forClass(List.class);
        verify(aiClient).sendBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void 조회_결과_없으면_AI_호출_없음() {
        OffsetDateTime since = OffsetDateTime.parse("2040-01-01T00:00:00Z");
        when(reviewRepo.findExportable(since)).thenReturn(List.of());

        int result = exporter.export(since);

        assertThat(result).isEqualTo(0);
        verifyNoInteractions(aiClient);
    }

    @Test
    void 누락된_application은_스킵하고_나머지_전송() {
        var review = reviewStub(3L, 999L, "APPROVE", "AUTO");
        OffsetDateTime since = OffsetDateTime.parse("2050-01-01T00:00:00Z");
        when(reviewRepo.findExportable(since)).thenReturn(List.of(review));
        when(applicationRepo.findById(999L)).thenReturn(Optional.empty());

        int result = exporter.export(since);

        assertThat(result).isEqualTo(1); // 조회 건수 기준
        verifyNoInteractions(aiClient);  // 배치 비어있어 전송 안 함
    }

    // ─────────────────────────────────────────────────────────────────────

    private static LoanReview reviewStub(Long revId, Long applId, String decision, String revType) {
        var r = mock(LoanReview.class);
        lenient().when(r.getRevId()).thenReturn(revId);
        lenient().when(r.getApplId()).thenReturn(applId);
        lenient().when(r.getRevDecisionCd()).thenReturn(decision);
        lenient().when(r.getRevTypeCd()).thenReturn(revType);
        return r;
    }

    private static LoanApplication appStub(Long applId, String emp, Long income,
                                           Long amount, int period, String purpose) {
        var a = mock(LoanApplication.class);
        lenient().when(a.getEmploymentTypeCd()).thenReturn(emp);
        lenient().when(a.getEstimatedIncomeAmt()).thenReturn(income);
        lenient().when(a.getRequestedAmount()).thenReturn(amount);
        lenient().when(a.getRequestedPeriodMo()).thenReturn(period);
        lenient().when(a.getLoanPurposeCd()).thenReturn(purpose);
        return a;
    }
}
