package com.bank.aigateway.llm.claude;

import com.bank.aigateway.llm.LlmClient;
import com.bank.aigateway.llm.LlmException;
import com.bank.aigateway.llm.LlmRequest;
import com.bank.aigateway.llm.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude")
@EnableConfigurationProperties(ClaudeProperties.class)
@RequiredArgsConstructor
public class ClaudeLlmClient implements LlmClient {

    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ClaudeProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public LlmResponse complete(LlmRequest request) {
        String body = buildBody(request);
        HttpClient client = buildHttpClient();

        for (int attempt = 1; attempt <= props.getMaxAttempts(); attempt++) {
            try {
                return doRequest(client, body);
            } catch (LlmException e) {
                if (attempt == props.getMaxAttempts()) throw e;
                log.warn("Claude 호출 실패 — attempt={}/{}: {}", attempt, props.getMaxAttempts(), e.getMessage());
                sleep(props.getRetryBackoffMs() * attempt);
            }
        }
        throw new LlmException("Claude 호출 최대 재시도 초과");
    }

    private LlmResponse doRequest(HttpClient client, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + MESSAGES_PATH))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", props.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new LlmException("Claude API 오류 — status=" + resp.statusCode() + " body=" + resp.body());
            }
            return parseResponse(resp.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Claude API 통신 오류", e);
        }
    }

    private String buildBody(LlmRequest request) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", props.getModel());
            root.put("max_tokens", request.maxTokens());

            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                root.put("system", request.systemPrompt());
            }

            ArrayNode messages = root.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            ArrayNode content = userMsg.putArray("content");
            ObjectNode textBlock = content.addObject();
            textBlock.put("type", "text");
            textBlock.put("text", request.userPrompt());

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("Claude 요청 직렬화 실패", e);
        }
    }

    private LlmResponse parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String text = root.path("content").get(0).path("text").asText();
            int inputTokens = root.path("usage").path("input_tokens").asInt();
            int outputTokens = root.path("usage").path("output_tokens").asInt();
            return new LlmResponse(text, inputTokens, outputTokens);
        } catch (Exception e) {
            throw new LlmException("Claude 응답 파싱 실패", e);
        }
    }

    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .build();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
