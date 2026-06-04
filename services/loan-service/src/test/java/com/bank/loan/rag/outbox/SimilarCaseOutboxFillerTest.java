package com.bank.loan.rag.outbox;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.repository.LoanReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SimilarCaseOutboxFiller 단위 테스트 — Phase E (E3-4).
 */
@ExtendWith(MockitoExtension.class)
class SimilarCaseOutboxFillerTest {

    @Mock
    private LoanReviewRepository reviewRepository;
    @Mock
    private LoanApplicationRepository applicationRepository;
    @Mock
    private SimilarCaseOutboxPublisher publisher;

    private final CaseOutboxProperties props =
            new CaseOutboxProperties(true, 1, 60000, 5000, 200, "loan-review.case-indexed.v1");

    private SimilarCaseOutboxFiller filler() {
        return new SimilarCaseOutboxFiller(reviewRepository, applicationRepository, publisher, props);
    }

    @Test
    void 완료_심사를_outbox로_적재() {
        LoanReview review = mock(LoanReview.class);
        when(review.getApplId()).thenReturn(10L);
        LoanApplication application = mock(LoanApplication.class);
        when(reviewRepository.findExportable(any())).thenReturn(List.of(review));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        filler().fill();

        verify(publisher).enqueueInCurrentTx(review, application);
    }

    @Test
    void 신청_미존재시_적재_스킵() {
        LoanReview review = mock(LoanReview.class);
        when(review.getApplId()).thenReturn(99L);
        when(reviewRepository.findExportable(any())).thenReturn(List.of(review));
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        filler().fill();

        verify(publisher, never()).enqueueInCurrentTx(any(), any());
    }

    @Test
    void 대상_없으면_아무것도_안함() {
        when(reviewRepository.findExportable(any())).thenReturn(List.of());

        filler().fill();

        verifyNoInteractions(publisher, applicationRepository);
    }
}
