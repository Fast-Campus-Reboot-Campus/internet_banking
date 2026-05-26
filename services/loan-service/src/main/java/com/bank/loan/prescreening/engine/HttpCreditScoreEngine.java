package com.bank.loan.prescreening.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * {@link CreditScoreEngine} 운영용 HTTP 어댑터 — 외부 신용평가사(KCB/NICE 등) API 호출.
 *
 * 활성 조건: {@code loan.credit-score.engine.type=http}.
 *
 * 회복탄력성:
 *   timeout         — connect/read 타임아웃 (기본 2s/3s)
 *   retry           — 5xx · IO 오류만 maxAttempts 회까지 재시도 (기본 3회, 지수 백오프 200ms)
 *                     4xx 는 즉시 실패(클라이언트 요청 자체가 잘못됐다는 신호이므로 재시도 무의미)
 *   fallback        — 모든 시도 실패 시 {@link CreditScoreEngineException} 으로 변환,
 *                     서비스 계층이 잡아 LOAN_029(503) 으로 노출
 *
 * Circuit Breaker 는 본 단계 범위 밖 — Resilience4j 도입 시 별도 추가.
 */
@Component
@ConditionalOnProperty(name = "loan.credit-score.engine.type", havingValue = "http")
public class HttpCreditScoreEngine implements CreditScoreEngine {

    private static final Logger log = LoggerFactory.getLogger(HttpCreditScoreEngine.class);

    private static final String EVALUATE_PATH = "/v1/credit-score";

    private final RestClient restClient;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public HttpCreditScoreEngine(
            RestClient.Builder builder,
            @Value("${loan.credit-score.engine.http.url}") String baseUrl,
            @Value("${loan.credit-score.engine.http.api-key:}") String apiKey) {
        this(builder, baseUrl, apiKey, 2000, 3000, 3, 200L);
    }

    /** 테스트·운영 튜닝용 풀 생성자. connect/read timeout 이 0 이하이면 builder 의 기본
     *  factory 를 유지(MockRestServiceServer 가 주입한 mock factory 보존을 위해 필요). */
    public HttpCreditScoreEngine(
            RestClient.Builder builder,
            String baseUrl,
            String apiKey,
            int connectTimeoutMs,
            int readTimeoutMs,
            int maxAttempts,
            long retryBackoffMs) {
        RestClient.Builder b;
        if (connectTimeoutMs > 0 || readTimeoutMs > 0) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (connectTimeoutMs > 0) factory.setConnectTimeout(connectTimeoutMs);
            if (readTimeoutMs > 0) factory.setReadTimeout(readTimeoutMs);
            b = builder.requestFactory(factory).baseUrl(baseUrl);
        } else {
            b = builder.baseUrl(baseUrl);
        }
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.restClient = b.build();
        this.maxAttempts = maxAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    public CreditScoreResult evaluate(CreditScoreRequest request) {
        Throwable lastEx = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                CreditScoreApiResponse response = restClient.post()
                        .uri(EVALUATE_PATH)
                        .body(CreditScoreApiRequest.of(request))
                        .retrieve()
                        .body(CreditScoreApiResponse.class);
                if (response == null) {
                    throw new CreditScoreEngineException("empty response body", null);
                }
                return response.toResult();
            } catch (HttpClientErrorException e) {
                // 4xx — 클라이언트/요청 자체 문제. 재시도 무의미하므로 즉시 실패.
                throw new CreditScoreEngineException(
                        "client error " + e.getStatusCode() + " from credit-score engine", e);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 5xx · IO/timeout — 일시 장애 가능성. 재시도.
                lastEx = e;
                log.warn("credit-score engine attempt {}/{} failed: {}",
                        attempt, maxAttempts, e.toString());
                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                }
            }
        }
        throw new CreditScoreEngineException(
                "credit-score engine failed after " + maxAttempts + " attempts", lastEx);
    }

    private void sleepBackoff(int attempt) {
        long delay = retryBackoffMs * (1L << (attempt - 1));  // 200 / 400 / 800 ms
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CreditScoreEngineException("retry interrupted", ie);
        }
    }
}
