package com.bank.ai.rag.ingestion.embedder;

/** 임베딩 어댑터(원격 호출·응답 검증) 단계 실패. */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
