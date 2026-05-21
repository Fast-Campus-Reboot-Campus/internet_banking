package com.bank.loan.document.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.document.domain.LoanDocument;
import com.bank.loan.document.dto.LoanDocumentDownload;
import com.bank.loan.document.dto.LoanDocumentListResponse;
import com.bank.loan.document.dto.LoanDocumentResponse;
import com.bank.loan.document.repository.LoanDocumentRepository;
import com.bank.loan.document.storage.DocumentStorage;
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

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_DOCUMENT";
    private static final String REASON_UPLOADED = "DOCUMENT_UPLOADED";
    private static final String REASON_DELETED  = "DOCUMENT_DELETED";

    private final LoanDocumentRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final DocumentStorage storage;
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
    public LoanDocumentDownload download(Long docId) {
        LoanDocument doc = repository.findByDocIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_041));
        return new LoanDocumentDownload(
                storage.load(doc.getDocUrl()),
                doc.getFileSizeBytes() == null ? -1L : doc.getFileSizeBytes(),
                doc.getMimeType(),
                doc.getDocName()
        );
    }

    @Transactional(readOnly = true)
    public LoanDocumentListResponse list(Long applId) {
        // 신청 활성 검증 — 미존재 신청 ID 로 빈 배열 반환 방지
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

        DocumentStorage.StoredFile stored = storage.store(application.getApplId(), file);

        LoanDocument saved = repository.save(LoanDocument.builder()
                .applId(application.getApplId())
                .docTypeCd(docTypeCd)
                .docStatusCd(LoanDocument.STATUS_UPLOADED)
                .docSourceCd(docSourceCd == null ? LoanDocument.SOURCE_MOBILE : docSourceCd)
                .docName(stored.originalName())
                .docUrl(stored.url())
                .docHash(stored.hash())
                .mimeType(stored.mimeType())
                .fileSizeBytes(stored.sizeBytes())
                .submittedAt(OffsetDateTime.now())
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getDocId(),
                null, LoanDocument.STATUS_UPLOADED,
                REASON_UPLOADED, "applId=" + application.getApplId() + ", docTypeCd=" + docTypeCd,
                currentActor.currentActorId()
        ));

        return LoanDocumentResponse.of(saved);
    }
}
