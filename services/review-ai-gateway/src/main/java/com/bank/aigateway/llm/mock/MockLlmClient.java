package com.bank.aigateway.llm.mock;

import com.bank.aigateway.llm.LlmClient;
import com.bank.aigateway.llm.LlmRequest;
import com.bank.aigateway.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(LlmRequest request) {
        log.info("[MockLlmClient] LLM 호출 (mock) — prompt 길이={}", request.userPrompt().length());
        String content = """
                {
                  "conclusion": "BIAS_SUSPECTED",
                  "reasoningSummary": "[Mock] 편향 의심 신호가 탐지되었습니다. 실제 LLM 연동 전 Mock 응답입니다.",
                  "confidenceScore": 0.5
                }
                """;
        return new LlmResponse(content, request.userPrompt().length() / 4, 80);
    }
}
