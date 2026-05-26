package com.bank.ai.rag.ingestion.embedder;

import java.util.List;

/**
 * 임베딩 모델 클라이언트 인터페이스.
 * provider(mock·openai·internal)는 application.yml rag.embed.provider 로 전환.
 */
public interface EmbeddingClient {

    /**
     * 텍스트 배치 → 벡터 배치 변환.
     * 입력 순서와 출력 순서는 동일하게 보장.
     *
     * @param texts 임베딩할 텍스트 목록
     * @return float[dimension] 벡터 목록
     */
    List<float[]> embed(List<String> texts);

    /** 이 클라이언트가 생성하는 벡터 차원 수 */
    int dimension();
}
