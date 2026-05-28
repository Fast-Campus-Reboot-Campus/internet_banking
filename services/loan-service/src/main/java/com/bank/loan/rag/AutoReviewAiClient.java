package com.bank.loan.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * auto-loan-review 내부 임베딩 배치 API 클라이언트 — D3-2.
 *
 * <p>POST {@code /api/internal/embeddings/batch} 로 청크 목록을 전달.
 * X-Internal-Token 헤더 인증 사용.
 */
@Slf4j
@Component
public class AutoReviewAiClient {

    private final RestClient restClient;
    private final SimilarCaseExportProperties props;

    public AutoReviewAiClient(SimilarCaseExportProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.autoReviewUrl())
                .build();
    }

    /**
     * 청크 목록을 auto-loan-review 에 일괄 전송.
     *
     * @param items 각 항목: corpus, sourceId, chunkSeq, chunkText, summary(null 가), metadata
     * @return upserted 건수
     */
    public int sendBatch(List<ChunkItemDto> items) {
        if (items.isEmpty()) return 0;

        var body = Map.of("items", items);
        try {
            var response = restClient.post()
                    .uri("/api/internal/embeddings/batch")
                    .header("X-Internal-Token", props.internalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object upserted = response.getBody().get("upserted");
                return upserted instanceof Number n ? n.intValue() : items.size();
            }
            log.warn("AutoReviewAiClient: 배치 응답 비정상 status={}", response.getStatusCode());
            return 0;
        } catch (Exception e) {
            log.error("AutoReviewAiClient: 배치 전송 실패 items={}", items.size(), e);
            throw new RuntimeException("similar_cases 배치 전송 실패", e);
        }
    }

    /** 배치 전송용 청크 DTO — JSON 직렬화 전용. */
    public record ChunkItemDto(
            String corpus,
            String sourceId,
            int chunkSeq,
            String chunkText,
            String summary,
            Map<String, Object> metadata
    ) {}
}
