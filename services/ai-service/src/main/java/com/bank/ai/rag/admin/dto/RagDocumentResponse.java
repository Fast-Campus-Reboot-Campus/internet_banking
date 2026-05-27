package com.bank.ai.rag.admin.dto;

import com.bank.ai.rag.document.domain.RagDocument;

import java.time.OffsetDateTime;

/**
 * RAG 문서 단건 응답 DTO.
 */
public record RagDocumentResponse(
        Long docId,
        String docTypeCd,
        String title,
        String sourceUri,
        String jurisdiction,
        String sensitivityCd,
        String docVersion,
        String effectiveFrom,
        String effectiveTo,
        String checksum,
        OffsetDateTime ingestedAt,
        OffsetDateTime createdAt
) {
    public static RagDocumentResponse from(RagDocument doc) {
        return new RagDocumentResponse(
                doc.getDocId(),
                doc.getDocTypeCd(),
                doc.getTitle(),
                doc.getSourceUri(),
                doc.getJurisdiction(),
                doc.getSensitivityCd(),
                doc.getDocVersion(),
                doc.getEffectiveFrom(),
                doc.getEffectiveTo(),
                doc.getChecksum(),
                doc.getIngestedAt(),
                doc.getCreatedAt()
        );
    }
}
