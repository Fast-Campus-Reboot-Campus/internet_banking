package com.bank.ai.rag.ingestion.embedder;

import com.bank.ai.rag.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAI <code>/v1/embeddings</code> 어댑터.
 *
 * 활성 조건: {@code rag.embed.provider=openai}.
 *
 * 회복탄력성:
 *   timeout   — connect/read 타임아웃 (기본 2s / 10s)
 *   retry     — 5xx · IO 오류만 maxAttempts 회까지 지수 백오프 재시도.
 *               4xx 는 즉시 실패(요청 자체가 잘못됐다는 신호이므로 재시도 무의미)
 *   dimension — 응답 벡터 차원이 {@code rag.embed.dimension} 과 다르면 예외 → 인덱스 정합성 보호
 *
 * 호출 단위:
 *   {@link #embed(List)} 한 번이 OpenAI 한 번 호출에 매핑된다.
 *   배치 분할은 상위 {@code IngestionService} 가 책임진다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rag.embed.provider", havingValue = "openai")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final String EMBED_PATH = "/v1/embeddings";

    private final RestClient restClient;
    private final String model;
    private final int dimension;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public OpenAiEmbeddingClient(RestClient.Builder builder, RagProperties ragProperties) {
        this(builder, ragProperties, ragProperties.embed().openai().connectTimeoutMs(),
                ragProperties.embed().openai().readTimeoutMs());
    }

    OpenAiEmbeddingClient(RestClient.Builder builder, RagProperties ragProperties,
                          int connectTimeoutMs, int readTimeoutMs) {
        var openai = ragProperties.embed().openai();
        RestClient.Builder b;
        if (connectTimeoutMs > 0 || readTimeoutMs > 0) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (connectTimeoutMs > 0) factory.setConnectTimeout(connectTimeoutMs);
            if (readTimeoutMs > 0) factory.setReadTimeout(readTimeoutMs);
            b = builder.requestFactory(factory).baseUrl(openai.baseUrl());
        } else {
            b = builder.baseUrl(openai.baseUrl());
        }
        if (openai.apiKey() != null && !openai.apiKey().isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openai.apiKey());
        }
        this.restClient = b.build();
        this.model = ragProperties.embed().model();
        this.dimension = ragProperties.embed().dimension();
        this.maxAttempts = openai.maxAttempts();
        this.retryBackoffMs = openai.retryBackoffMs();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        OpenAiEmbeddingResponse response = callWithRetry(new OpenAiEmbeddingRequest(model, texts, dimension));
        return toOrderedVectors(response, texts.size());
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private OpenAiEmbeddingResponse callWithRetry(OpenAiEmbeddingRequest request) {
        Throwable lastEx = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                OpenAiEmbeddingResponse body = restClient.post()
                        .uri(EMBED_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OpenAiEmbeddingResponse.class);
                if (body == null || body.data() == null || body.data().isEmpty()) {
                    throw new EmbeddingException("openai 응답 본문 비어 있음");
                }
                return body;
            } catch (HttpClientErrorException e) {
                throw new EmbeddingException(
                        "openai client error " + e.getStatusCode() + " — 요청 형식 점검 필요", e);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                lastEx = e;
                log.warn("[embed:openai] attempt {}/{} failed: {}", attempt, maxAttempts, e.toString());
                if (attempt < maxAttempts) sleepBackoff(attempt);
            }
        }
        throw new EmbeddingException("openai 임베딩 " + maxAttempts + "회 재시도 모두 실패", lastEx);
    }

    /** OpenAI 응답의 {@code index} 기준으로 입력 순서와 동일하게 정렬해 반환. */
    private List<float[]> toOrderedVectors(OpenAiEmbeddingResponse response, int expectedSize) {
        if (response.data().size() != expectedSize) {
            throw new EmbeddingException("openai 응답 개수 불일치: expected=" + expectedSize
                    + " actual=" + response.data().size());
        }
        float[][] ordered = new float[expectedSize][];
        for (OpenAiEmbeddingResponse.Item item : response.data()) {
            if (item.index() < 0 || item.index() >= expectedSize) {
                throw new EmbeddingException("openai 응답 index 범위 초과: " + item.index());
            }
            float[] vector = toFloatArray(item.embedding());
            if (vector.length != dimension) {
                throw new EmbeddingException("openai 응답 차원 불일치: expected=" + dimension
                        + " actual=" + vector.length);
            }
            ordered[item.index()] = vector;
        }
        for (int i = 0; i < expectedSize; i++) {
            if (ordered[i] == null) {
                throw new EmbeddingException("openai 응답 누락 index=" + i);
            }
        }
        return Arrays.asList(ordered);
    }

    private float[] toFloatArray(List<Double> values) {
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) out[i] = values.get(i).floatValue();
        return out;
    }

    private void sleepBackoff(int attempt) {
        long delay = retryBackoffMs * (1L << (attempt - 1));   // 300 / 600 / 1200 ms
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("openai 재시도 대기 중 인터럽트", ie);
        }
    }

    /**
     * OpenAI 임베딩 요청 페이로드.
     * dimensions 는 text-embedding-3-* 계열에서만 의미가 있으나, 다른 모델은 무시하므로 항상 전달.
     */
    record OpenAiEmbeddingRequest(String model, List<String> input, int dimensions) {}

    record OpenAiEmbeddingResponse(List<Item> data, String model) {
        record Item(int index, List<Double> embedding) {}
    }
}
