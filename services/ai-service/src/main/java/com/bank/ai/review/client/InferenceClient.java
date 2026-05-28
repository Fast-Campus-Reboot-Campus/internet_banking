package com.bank.ai.review.client;

import com.bank.ai.review.client.dto.InferenceRequest;
import com.bank.ai.review.client.dto.InferenceResponse;
import com.bank.ai.support.AiErrorCode;
import com.bank.common.web.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class InferenceClient {

    private final RestClient restClient;

    public InferenceClient(
            RestClient.Builder builder,
            @Value("${ai.inference.base-url:http://localhost:8090}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        log.info("InferenceClient configured: baseUrl={}", baseUrl);
    }

    public InferenceResponse predict(InferenceRequest req) {
        try {
            return restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("inference /predict error: status={}", response.getStatusCode());
                        throw new BusinessException(AiErrorCode.INFERENCE_FAILED);
                    })
                    .body(InferenceResponse.class);
        } catch (ResourceAccessException e) {
            log.error("inference server unreachable: {}", e.getMessage());
            throw new BusinessException(AiErrorCode.INFERENCE_UNAVAILABLE);
        }
    }
}
