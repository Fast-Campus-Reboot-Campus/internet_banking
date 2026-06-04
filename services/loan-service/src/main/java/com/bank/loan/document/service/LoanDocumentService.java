package com.bank.loan.document.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bank.loan.document.domain.LoanDocument;
import com.bank.loan.document.docagent.DocAgentClient;
import com.bank.loan.document.docagent.SubmissionResult;
import com.bank.loan.document.domain.LoanDocumentSubmission;
import com.bank.loan.document.dto.LoanDocumentListResponse;
import com.bank.loan.document.dto.LoanDocumentResponse;
import com.bank.loan.document.repository.LoanDocumentRepository;
import com.bank.loan.document.repository.LoanDocumentSubmissionRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanDocumentService {

    private static final Logger log = LoggerFactory.getLogger(LoanDocumentService.class);

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_DOCUMENT";
    private static final String REASON_DELETED         = "DOCUMENT_DELETED";
    private static final String REASON_VERIFIED        = "DOCUMENT_VERIFIED";
    private static final String REASON_NEEDS_RESUBMIT  = "DOCUMENT_NEEDS_RESUBMIT";
    private static final String REASON_HOLD            = "DOCUMENT_HOLD";

    private final LoanDocumentRepository repository;
    private final LoanDocumentSubmissionRepository submissionRepository;
    private final LoanApplicationRepository applicationRepository;
    private final DocAgentClient docAgentClient;
    private final CurrentActorProvider currentActor;
    private final StatusHistoryPublisher statusHistoryPublisher;

    @Transactional
    public LoanDocumentResponse delete(Long docId) {
        LoanDocument doc = repository.findByDocIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_041));
        String before = doc.getDocStatusCd();
        doc.markDeleted();
        doc.softDelete(currentActor.currentActorId());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, doc.getDocId(),
                before, LoanDocument.STATUS_DELETED,
                REASON_DELETED, null,
                currentActor.currentActorId()
        ));

        return LoanDocumentResponse.of(doc);
    }

    @Transactional(readOnly = true)
    public LoanDocumentListResponse list(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        List<LoanDocumentResponse> items = repository
                .findByApplIdAndDeletedAtIsNullOrderBySubmittedAtAsc(applId)
                .stream().map(LoanDocumentResponse::of).toList();
        return LoanDocumentListResponse.of(items);
    }

    @Transactional
    public LoanDocumentResponse upload(Long applId, String docTypeCd, String docSourceCd,
                                       MultipartFile file) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanDocument saved = repository.save(LoanDocument.builder()
                .applId(application.getApplId())
                .docTypeCd(docTypeCd)
                .docStatusCd(LoanDocument.STATUS_UPLOADED)
                .docSourceCd(docSourceCd == null ? LoanDocument.SOURCE_MOBILE : docSourceCd)
                .docName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .submittedAt(OffsetDateTime.now())
                .build());

        SubmissionResult result = docAgentClient.submit(
                application.getApplNo(),
                docTypeCd,
                String.valueOf(application.getProdId()),
                file);

        saved.applyVerifyResult(result.verifyStatus(), result.submissionId());

        Double rawScore = result.documentVerification() != null
                ? result.documentVerification().confidenceScore() : null;
        submissionRepository.save(LoanDocumentSubmission.builder()
                .submissionId(result.submissionId())
                .docId(saved.getDocId())
                .applicationId(application.getApplNo())
                .docCode(docTypeCd)
                .verifyStatus(result.verifyStatus())
                .confidenceScore(rawScore != null
                        ? java.math.BigDecimal.valueOf(rawScore) : null)
                .occurredAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build());

        String reason = switch (result.verifyStatus()) {
            case SubmissionResult.VERIFY_AUTO_PASS      -> REASON_VERIFIED;
            case SubmissionResult.VERIFY_NEEDS_RESUBMIT -> REASON_NEEDS_RESUBMIT;
            default                                     -> REASON_HOLD;
        };

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getDocId(),
                LoanDocument.STATUS_UPLOADED, saved.getDocStatusCd(),
                reason, "submissionId=" + result.submissionId(),
                currentActor.currentActorId()
        ));

        return LoanDocumentResponse.of(saved);
    }

}
