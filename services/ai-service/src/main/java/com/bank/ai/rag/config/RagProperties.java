package com.bank.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml rag.* 설정 바인딩.
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(Embed embed, Chunk chunk, Corpus corpus) {

    public record Embed(
            String provider,   // mock | openai | internal
            String model,
            int dimension,
            int batchSize,
            long timeoutMs
    ) {}

    public record Chunk(
            int size,          // 청크당 최대 토큰(단어) 수
            int overlap        // 슬라이딩 윈도우 겹침 크기
    ) {}

    public record Corpus(
            String baseDir     // 코퍼스 루트 경로 (docs/corpus 기준)
    ) {}
}
