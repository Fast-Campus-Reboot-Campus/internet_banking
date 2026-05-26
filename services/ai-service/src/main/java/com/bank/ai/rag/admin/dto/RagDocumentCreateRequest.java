package com.bank.ai.rag.admin.dto;

/**
 * RAG 문서 단건 등록 요청.
 * sourceUri 형식: {@code corpus://relative/path/to/file.pdf}
 * effectiveFrom / effectiveTo 형식: {@code YYYYMMDD} 또는 null
 */
public record RagDocumentCreateRequest(
        String docTypeCd,       // LAW · POLICY · PRODUCT_TERMS · 등
        String title,
        String sourceUri,       // corpus://... 상대 경로
        String jurisdiction,    // 생략 시 KR
        String sensitivityCd,   // 생략 시 PUBLIC
        String docVersion,
        String effectiveFrom,   // YYYYMMDD
        String effectiveTo      // YYYYMMDD, 현행 유효 시 null
) {}
