package com.bank.ai.review.client;

import com.bank.ai.review.dto.ReviewReportUpdateRequest;
import com.bank.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * loan-service 로의 콜백 클라이언트. LLM 리포트 생성 결과를 전송한다.
 */
@Slf4j
@Component
public class LoanServiceClient {

    private final RestClient restClient;

    public LoanServiceClient(@Value("${ai.loan-service.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void updateReport(Long revId, ReviewReportUpdateRequest req) {
        log.info("Sending report update callback to loan-service: revId={}", revId);
        restClient.patch()
                .uri("/api/loan-applications/reviews/{revId}/report", revId)
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}
