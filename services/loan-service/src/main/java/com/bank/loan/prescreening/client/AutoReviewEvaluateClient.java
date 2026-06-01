package com.bank.loan.prescreening.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * auto-loan-review 자동심사 평가 API 클라이언트.
 *
 * <p>POST {@code /api/ai/auto-review/evaluate} — Track 분기(TRACK_1/2/3), PD 스코어, 결정 근거 반환.
 * X-Internal-Token 헤더 인증 사용.
 *
 * <p>장애 시 {@link RestClientException} 을 그대로 전파 — 호출 측에서 try-catch 로 장애 격리.
 */
@Slf4j
@Component
public class AutoReviewEvaluateClient {

    private static final String EVALUATE_PATH = "/api/ai/auto-review/evaluate";

    private final AutoReviewProperties props;
    private final RestClient restClient;

    public AutoReviewEvaluateClient(RestClient.Builder builder, AutoReviewProperties props) {
        this.props = props;
        RestClient.Builder b;
        if (props.connectTimeoutMs() > 0 || props.readTimeoutMs() > 0) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (props.connectTimeoutMs() > 0) factory.setConnectTimeout(props.connectTimeoutMs());
            if (props.readTimeoutMs()    > 0) factory.setReadTimeout(props.readTimeoutMs());
            b = builder.requestFactory(factory).baseUrl(props.baseUrl());
        } else {
            b = builder.baseUrl(props.baseUrl());
        }
        this.restClient = b.build();
    }

    public AutoReviewEvaluateResult evaluate(AutoReviewEvaluateRequest req) {
        if (!props.enabled()) {
            log.debug("auto-review 비활성화 — evaluate 건너뜀");
            return null;
        }
        log.debug("auto-review evaluate 호출 productCode={} creditScore={}",
                req.productCode(), req.creditScoreProxy());
        AutoReviewEvaluateResult result = restClient.post()
                .uri(EVALUATE_PATH)
                .header("X-Internal-Token", props.internalToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(AutoReviewEvaluateResult.class);
        log.info("auto-review evaluate 완료 track={} pd={}", result != null ? result.track() : null,
                result != null ? result.pd() : null);
        return result;
    }
}
