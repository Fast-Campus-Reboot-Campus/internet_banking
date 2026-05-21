package com.bank.loan.prescreening.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link CreditScoreEngine} 운영용 HTTP 어댑터 — 외부 신용평가사(KCB/NICE 등) API 호출.
 *
 * 활성 조건: {@code loan.credit-score.engine.type=http}.
 * 설정:
 *   loan.credit-score.engine.http.url     — 외부 API base URL
 *   loan.credit-score.engine.http.api-key — Bearer 토큰(선택, 비어있으면 헤더 미부착)
 *
 * timeout/재시도/회로차단기 정책은 본 단계 범위 밖 — 후속 회복탄력성 작업에서 도입.
 */
@Component
@ConditionalOnProperty(name = "loan.credit-score.engine.type", havingValue = "http")
public class HttpCreditScoreEngine implements CreditScoreEngine {

    private static final String EVALUATE_PATH = "/v1/credit-score";

    private final RestClient restClient;

    public HttpCreditScoreEngine(
            RestClient.Builder builder,
            @Value("${loan.credit-score.engine.http.url}") String baseUrl,
            @Value("${loan.credit-score.engine.http.api-key:}") String apiKey) {
        RestClient.Builder b = builder.baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.restClient = b.build();
    }

    @Override
    public CreditScoreResult evaluate(CreditScoreRequest request) {
        CreditScoreApiResponse response = restClient.post()
                .uri(EVALUATE_PATH)
                .body(CreditScoreApiRequest.of(request))
                .retrieve()
                .body(CreditScoreApiResponse.class);
        if (response == null) {
            throw new IllegalStateException("credit-score engine returned empty body");
        }
        return response.toResult();
    }
}
