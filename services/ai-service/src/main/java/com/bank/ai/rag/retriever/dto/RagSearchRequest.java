package com.bank.ai.rag.retriever.dto;

/**
 * RAG 검색 요청.
 *
 * @param query        자연어 질문
 * @param profile      product | review | bias-audit
 * @param sensitivityCd PUBLIC·INTERNAL·RESTRICTED (null = 제한 없음)
 * @param asOfDate     기준일 YYYYMMDD (null = 현재 유효 문서만)
 * @param topK         반환할 최대 청크 수 (null = 기본값 5)
 */
public record RagSearchRequest(
        String query,
        String profile,
        String sensitivityCd,
        String asOfDate,
        Integer topK
) {}
