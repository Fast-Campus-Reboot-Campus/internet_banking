package com.bank.loan.document.dto;

import com.bank.loan.document.domain.LoanDocument;

import java.time.OffsetDateTime;

public record LoanDocumentResponse(
        Long docId,
        Long applId,
        String docTypeCd,
        String docStatusCd,
        String docSourceCd,
        String docName,
        String docUrl,
        String docHash,
        String mimeType,
        Long fileSizeBytes,
        OffsetDateTime submittedAt,
        OffsetDateTime verifiedAt,
        String verifyResultCd,
        String retentionUntil
) {
    public static LoanDocumentResponse of(LoanDocument d) {
        return new LoanDocumentResponse(
                d.getDocId(), d.getApplId(),
                d.getDocTypeCd(), d.getDocStatusCd(), d.getDocSourceCd(),
                d.getDocName(), d.getDocUrl(), d.getDocHash(),
                d.getMimeType(), d.getFileSizeBytes(),
                d.getSubmittedAt(), d.getVerifiedAt(), d.getVerifyResultCd(),
                d.getRetentionUntil()
        );
    }
}
