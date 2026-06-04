package com.bank.loan.rag.outbox;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.review.domain.LoanReview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SimilarCaseOutboxPublisher 단위 테스트 — Phase E (E3-4).
 * repository mock — PII-free 페이로드 구성·멱등 스킵 검증.
 */
@ExtendWith(MockitoExtension.class)
class SimilarCaseOutboxPublisherTest {

    @Mock
    private LoanReviewOutboxRepository repository;

    @InjectMocks
    private SimilarCaseOutboxPublisher publisher;

    private LoanReview review() {
        LoanReview review = mock(LoanReview.class);
        when(review.getRevId()).thenReturn(100L);
        lenient().when(review.getRevTypeCd()).thenReturn("AUTO");
        lenient().when(review.getRevDecisionCd()).thenReturn("APPROVE");
        return review;
    }

    private LoanApplication application() {
        LoanApplication application = mock(LoanApplication.class);
        lenient().when(application.getEmploymentTypeCd()).thenReturn("EMPLOYEE");
        lenient().when(application.getEstimatedIncomeAmt()).thenReturn(60_000_000L);
        lenient().when(application.getRequestedAmount()).thenReturn(150_000_000L);
        lenient().when(application.getRequestedPeriodMo()).thenReturn(360);
        lenient().when(application.getLoanPurposeCd()).thenReturn("HOUSE");
        return application;
    }

    @Test
    void 신규_케이스_outbox_적재() {
        when(repository.existsByIdempotencyKey("CASE_INDEXED:100")).thenReturn(false);

        publisher.enqueueInCurrentTx(review(), application());

        ArgumentCaptor<LoanReviewOutbox> captor = ArgumentCaptor.forClass(LoanReviewOutbox.class);
        verify(repository).save(captor.capture());
        LoanReviewOutbox saved = captor.getValue();

        assertThat(saved.getAggregateId()).isEqualTo(100L);
        assertThat(saved.getStatus()).isEqualTo(LoanReviewOutbox.STATUS_PENDING);
        assertThat(saved.getPayload())
                .contains("\"source_id\":\"rev-100\"")
                .contains("similar_cases")
                .contains("APPROVE");
    }

    @Test
    void 중복_키_존재시_적재_스킵() {
        when(repository.existsByIdempotencyKey("CASE_INDEXED:100")).thenReturn(true);

        publisher.enqueueInCurrentTx(review(), application());

        verify(repository, never()).save(any());
    }
}
