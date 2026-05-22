package com.bank.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml rag.* 설정 바인딩.
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(Embed embed, Chunk chunk, Corpus corpus, Scheduler scheduler) {

    public record Embed(
            String provider,   // mock | openai | internal
            String model,
            int dimension,
            int batchSize,
            long timeoutMs,
            Openai openai
    ) {}

    /**
     * OpenAI 임베딩 API 어댑터 설정. provider=openai 일 때만 사용.
     * apiKey 가 비어 있으면 Authorization 헤더 미부착 — 사내 프록시·게이트웨이 경로 대응.
     */
    public record Openai(
            String baseUrl,        // 기본: https://api.openai.com
            String apiKey,         // env: OPENAI_API_KEY
            int connectTimeoutMs,
            int readTimeoutMs,
            int maxAttempts,       // 5xx/IO 재시도 횟수
            long retryBackoffMs    // 지수 백오프 base (ms)
    ) {}

    public record Chunk(
            int size,          // 청크당 최대 토큰(단어) 수
            int overlap        // 슬라이딩 윈도우 겹침 크기
    ) {}

    public record Corpus(
            String baseDir     // 코퍼스 루트 경로 (docs/corpus 기준)
    ) {}

    /**
     * RAG 정기 인제스트 스케줄러 설정.
     * enabled=false 면 @Scheduled bean 자체가 등록되지 않아 운영 안전.
     */
    public record Scheduler(
            boolean enabled,
            String  cron       // 예: "0 0 3 * * *" — 매일 새벽 3시
    ) {}
}
