package com.bank.ai.rag.retriever.dto;

import java.util.List;

/**
 * RAG 검색 응답.
 */
public record RagSearchResponse(
        String          query,
        String          profile,
        List<RagChunkHit> hits
) {}
