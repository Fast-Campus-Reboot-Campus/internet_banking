package com.bank.ai.rag.retriever.dto;

/**
 * 벡터 검색 단건 결과.
 * score 는 코사인 유사도 (0.0 ~ 1.0, 높을수록 유사).
 */
public record RagChunkHit(
        Long   chunkId,
        Long   docId,
        String docTypeCd,
        String title,
        String sourceUri,
        int    chunkSeq,
        String content,
        double score
) {}
