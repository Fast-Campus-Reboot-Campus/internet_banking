package com.bank.loan.advisory.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ai-service RAG 검색 HTTP 클라이언트 (시나리오 δ).
 *
 * POST {ai-service}/rag/search 를 호출해 청크 목록을 반환한다.
 * advisory-service 가 자체 JDBC 쿼리 대신 이 클라이언트를 사용하도록 전환.
 */
@Slf4j
@Component
public class AiServiceRagClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(15);

    private final String      baseUrl;
    private final HttpClient  httpClient;
    private final ObjectMapper mapper;

    public AiServiceRagClient(
            @Value("${ai-service.base-url:http://localhost:8086}") String baseUrl,
            ObjectMapper mapper) {
        this.baseUrl    = baseUrl;
        this.mapper     = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public record ChunkHit(
            Long   chunkId,
            Long   docId,
            String docTypeCd,
            String title,
            String sourceUri,
            int    chunkSeq,
            String content,
            double score
    ) {}

    /**
     * @param query   검색 쿼리 텍스트
     * @param profile review | bias-audit | similar-case
     * @param topK    최대 결과 수
     */
    public List<ChunkHit> search(String query, String profile, int topK) {
        try {
            String body = mapper.writeValueAsString(new RagSearchPayload(query, profile, null, null, topK));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/rag/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(REQUEST_TIMEOUT)
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("[AiServiceRagClient] 비정상 응답 status={}", resp.statusCode());
                return List.of();
            }

            JsonNode root = mapper.readTree(resp.body());
            JsonNode hitsNode = root.path("hits");
            List<ChunkHit> hits = new ArrayList<>();
            for (JsonNode h : hitsNode) {
                hits.add(new ChunkHit(
                        h.path("chunkId").asLong(),
                        h.path("docId").asLong(),
                        h.path("docTypeCd").asText(""),
                        h.path("title").asText(""),
                        h.path("sourceUri").asText(""),
                        h.path("chunkSeq").asInt(),
                        h.path("content").asText(""),
                        h.path("score").asDouble()
                ));
            }
            return hits;

        } catch (Exception e) {
            log.warn("[AiServiceRagClient] 검색 실패 (빈 결과 반환): {}", e.getMessage());
            return List.of();
        }
    }

    private record RagSearchPayload(
            String query,
            String profile,
            String sensitivityCd,
            String asOfDate,
            Integer topK
    ) {}
}
