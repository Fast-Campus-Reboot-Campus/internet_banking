package com.bank.ai.langfuse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "langfuse.enabled", havingValue = "true")
@EnableConfigurationProperties(LangfuseProperties.class)
public class LangfuseService {

    private final RestClient restClient;

    public LangfuseService(LangfuseProperties props) {
        String credentials = props.publicKey() + ":" + props.secretKey();
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        this.restClient = RestClient.builder()
                .baseUrl(props.host())
                .defaultHeader("Authorization", authHeader)
                .build();
    }

    public String newTraceId() {
        return UUID.randomUUID().toString();
    }

    public void trace(String traceId, String name, Map<String, Object> metadata) {
        send(Map.of(
                "id", UUID.randomUUID().toString(),
                "type", "trace-create",
                "timestamp", Instant.now().toString(),
                "body", Map.of(
                        "id", traceId,
                        "name", name,
                        "metadata", metadata != null ? metadata : Map.of()
                )
        ));
    }

    public void span(String traceId, String name, Object input, Object output,
                     Instant startTime, Instant endTime) {
        send(Map.of(
                "id", UUID.randomUUID().toString(),
                "type", "span-create",
                "timestamp", Instant.now().toString(),
                "body", Map.of(
                        "id", UUID.randomUUID().toString(),
                        "traceId", traceId,
                        "name", name,
                        "input", input,
                        "output", output,
                        "startTime", startTime.toString(),
                        "endTime", endTime.toString()
                )
        ));
    }

    private void send(Map<String, Object> event) {
        try {
            restClient.post()
                    .uri("/api/public/ingestion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("batch", List.of(event)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Langfuse 전송 실패: {}", e.getMessage());
        }
    }
}
