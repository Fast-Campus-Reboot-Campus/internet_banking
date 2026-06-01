package com.bank.loan.document.docagent;

import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.document.domain.LoanDocument;
import com.bank.loan.document.domain.LoanDocumentSubmission;
import com.bank.loan.document.repository.LoanDocumentRepository;
import com.bank.loan.document.repository.LoanDocumentSubmissionRepository;
import com.bank.loan.document.service.LoanDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoanDocumentService — doc-agent 연동 단위 테스트 (Spring 컨텍스트 없음).
 *
 * 검증 포인트:
 *   1) upload() AUTO_PASS → LoanDocument VERIFIED, loan_document_submission insert
 *   2) upload() NEEDS_RESUBMIT → LoanDocument REJECTED
 *   3) upload() HOLD → LoanDocument UPLOADED 유지
 *   4) handleRoutedEvent() → 기존 LoanDocument 상태 업데이트
 *   5) handleFraudAuditEvent() → LoanApplication REJECTED, retention_until 설정
 */
@ExtendWith(MockitoExtension.class)
class LoanDocumentServiceDocAgentTest {

    @Mock LoanDocumentRepository           documentRepository;
    @Mock LoanDocumentSubmissionRepository submissionRepository;
    @Mock LoanApplicationRepository        applicationRepository;
    @Mock DocAgentClient                   docAgentClient;
    @Mock CurrentActorProvider             currentActor;
    @Mock StatusHistoryPublisher           statusHistoryPublisher;

    private LoanDocumentService service;

    @BeforeEach
    void setUp() {
        service = new LoanDocumentService(
                documentRepository, submissionRepository,
                applicationRepository, docAgentClient,
                currentActor, statusHistoryPublisher);
    }

    // ─── upload() ────────────────────────────────────────────────────────────

    @Test
    void upload_AUTO_PASS_이면_LoanDocument_VERIFIED_로_전이() {
        LoanApplication appl = fakeApplication();
        when(applicationRepository.findByApplIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(appl));

        LoanDocument savedDoc = fakeDocument(1L);
        when(documentRepository.save(any())).thenReturn(savedDoc);
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(docAgentClient.submit(any(), any(), any(), any()))
                .thenReturn(result("sub-001", SubmissionResult.VERIFY_AUTO_PASS, 0.95));

        service.upload(1L, "EMPLOYMENT_CERT", null, mockFile());

        assertThat(savedDoc.getDocStatusCd()).isEqualTo(LoanDocument.STATUS_VERIFIED);
        assertThat(savedDoc.getVerifyResultCd()).isEqualTo(SubmissionResult.VERIFY_AUTO_PASS);
        assertThat(savedDoc.getDocUrl()).isEqualTo("sub-001");
    }

    @Test
    void upload_AUTO_PASS_이면_loan_document_submission_insert() {
        LoanApplication appl = fakeApplication();
        when(applicationRepository.findByApplIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(appl));
        when(documentRepository.save(any())).thenReturn(fakeDocument(1L));
        when(docAgentClient.submit(any(), any(), any(), any()))
                .thenReturn(result("sub-001", SubmissionResult.VERIFY_AUTO_PASS, 0.95));

        service.upload(1L, "EMPLOYMENT_CERT", null, mockFile());

        ArgumentCaptor<LoanDocumentSubmission> captor = ArgumentCaptor.forClass(LoanDocumentSubmission.class);
        verify(submissionRepository).save(captor.capture());
        LoanDocumentSubmission sub = captor.getValue();
        assertThat(sub.getSubmissionId()).isEqualTo("sub-001");
        assertThat(sub.getVerifyStatus()).isEqualTo(SubmissionResult.VERIFY_AUTO_PASS);
        assertThat(sub.getDocCode()).isEqualTo("EMPLOYMENT_CERT");
    }

    @Test
    void upload_NEEDS_RESUBMIT_이면_LoanDocument_REJECTED_로_전이() {
        when(applicationRepository.findByApplIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(fakeApplication()));
        LoanDocument savedDoc = fakeDocument(1L);
        when(documentRepository.save(any())).thenReturn(savedDoc);
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(docAgentClient.submit(any(), any(), any(), any()))
                .thenReturn(result("sub-002", SubmissionResult.VERIFY_NEEDS_RESUBMIT, 0.3));

        service.upload(1L, "EMPLOYMENT_CERT", null, mockFile());

        assertThat(savedDoc.getDocStatusCd()).isEqualTo(LoanDocument.STATUS_REJECTED);
        assertThat(savedDoc.getVerifyResultCd()).isEqualTo(SubmissionResult.VERIFY_NEEDS_RESUBMIT);
    }

    @Test
    void upload_HOLD_이면_LoanDocument_상태_UPLOADED_유지() {
        when(applicationRepository.findByApplIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(fakeApplication()));
        LoanDocument savedDoc = fakeDocument(1L);
        when(documentRepository.save(any())).thenReturn(savedDoc);
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(docAgentClient.submit(any(), any(), any(), any()))
                .thenReturn(result("sub-003", SubmissionResult.VERIFY_HOLD, 0.6));

        service.upload(1L, "EMPLOYMENT_CERT", null, mockFile());

        assertThat(savedDoc.getDocStatusCd()).isEqualTo(LoanDocument.STATUS_UPLOADED);
        assertThat(savedDoc.getVerifyResultCd()).isEqualTo(SubmissionResult.VERIFY_HOLD);
    }

    // ─── handleRoutedEvent() ──────────────────────────────────────────────────

    @Test
    void handleRoutedEvent_AUTO_PASS_이면_서류_VERIFIED_로_업데이트() {
        LoanDocument doc = fakeDocument(2L);
        doc.applyVerifyResult(SubmissionResult.VERIFY_HOLD, "sub-routed");  // 초기: HOLD 상태
        when(documentRepository.findByDocUrlAndDeletedAtIsNull("sub-routed"))
                .thenReturn(Optional.of(doc));

        service.handleRoutedEvent("sub-routed", SubmissionResult.VERIFY_AUTO_PASS);

        assertThat(doc.getDocStatusCd()).isEqualTo(LoanDocument.STATUS_VERIFIED);
        assertThat(doc.getVerifyResultCd()).isEqualTo(SubmissionResult.VERIFY_AUTO_PASS);
    }

    @Test
    void handleRoutedEvent_서류_없으면_예외_없이_스킵() {
        when(documentRepository.findByDocUrlAndDeletedAtIsNull("not-exist"))
                .thenReturn(Optional.empty());

        // 예외 발생 없이 종료
        service.handleRoutedEvent("not-exist", SubmissionResult.VERIFY_AUTO_PASS);
    }

    // ─── handleFraudAuditEvent() ──────────────────────────────────────────────

    @Test
    void handleFraudAuditEvent_LoanApplication_REJECTED_전이() {
        LoanApplication appl = fakeApplication();
        when(applicationRepository.findByApplNoAndDeletedAtIsNull("LOAN-2026-001"))
                .thenReturn(Optional.of(appl));
        when(documentRepository.findByDocUrlAndDeletedAtIsNull("sub-fraud"))
                .thenReturn(Optional.empty());

        service.handleFraudAuditEvent("sub-fraud", "LOAN-2026-001", "2031-05-28");

        assertThat(appl.currentStatus()).isEqualTo(LoanApplication.STATUS_REJECTED);
    }

    @Test
    void handleFraudAuditEvent_서류_retention_until_설정() {
        when(applicationRepository.findByApplNoAndDeletedAtIsNull("LOAN-2026-001"))
                .thenReturn(Optional.of(fakeApplication()));
        LoanDocument doc = fakeDocument(3L);
        doc.applyVerifyResult(SubmissionResult.VERIFY_HOLD, "sub-fraud");
        when(documentRepository.findByDocUrlAndDeletedAtIsNull("sub-fraud"))
                .thenReturn(Optional.of(doc));

        service.handleFraudAuditEvent("sub-fraud", "LOAN-2026-001", "2031-05-28");

        assertThat(doc.getRetentionUntil()).isEqualTo("2031-05-28");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private LoanApplication fakeApplication() {
        return LoanApplication.builder()
                .applId(1L).applNo("LOAN-2026-001").customerId(100L)
                .prodId(1L).channelCd("MOBILE")
                .requestedAmount(10_000_000L).requestedPeriodMo(12)
                .applStatusCd(LoanApplication.STATUS_SUBMITTED)
                .build();
    }

    private LoanDocument fakeDocument(Long docId) {
        return LoanDocument.builder()
                .docId(docId).applId(1L)
                .docTypeCd("EMPLOYMENT_CERT")
                .docStatusCd(LoanDocument.STATUS_UPLOADED)
                .docSourceCd(LoanDocument.SOURCE_MOBILE)
                .build();
    }

    private SubmissionResult result(String submissionId, String verifyStatus, double score) {
        return new SubmissionResult(
                submissionId, "LOAN-2026-001", "EMPLOYMENT_CERT",
                verifyStatus,
                new SubmissionResult.DocumentVerification(score));
    }

    private MockMultipartFile mockFile() {
        return new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
    }
}
