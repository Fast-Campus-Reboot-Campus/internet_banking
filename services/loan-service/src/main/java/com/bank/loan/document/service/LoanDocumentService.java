package com.bank.loan.document.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.document.domain.LoanDocument;
import com.bank.loan.document.dto.LoanDocumentResponse;
import com.bank.loan.document.repository.LoanDocumentRepository;
import com.bank.loan.document.storage.DocumentStorage;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LoanDocumentService {

    private final LoanDocumentRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final DocumentStorage storage;

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

        return LoanDocumentResponse.of(saved);
    }
}
