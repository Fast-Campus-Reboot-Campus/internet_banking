package com.bank.loan.advisory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class AdvisoryClient {

    private static final Logger log = LoggerFactory.getLogger(AdvisoryClient.class);
    private static final String REPORTS_PATH = "/api/advisory/reports";

    private final RestClient restClient;

    public AdvisoryClient(RestClient.Builder builder, AdvisoryProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.timeoutMs());
        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(props.baseUrl())
                .build();
    }

    /**
     * revId 에 연결된 Advisory 리포트 목록 조회.
     * advisory-service 장애 시 빈 목록 반환(fail-open) — 심사 프로세스를 차단하지 않는다.
     */
    public List<AdvisoryReportSummary> getReports(Long revId) {
        try {
            List<AdvisoryReportSummary> result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(REPORTS_PATH)
                            .queryParam("revId", revId)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("advisory-service 조회 실패 revId={}: {}", revId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
