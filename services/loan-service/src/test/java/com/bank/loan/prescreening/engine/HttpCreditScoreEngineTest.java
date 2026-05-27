package com.bank.loan.prescreening.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * HttpCreditScoreEngine 단위 테스트 — Spring 컨텍스트 없이 RestTemplate + MockRestServiceServer
 * 로 외부 API 응답 stub.
 *
 * 검증 포인트:
 *   1) 우리 도메인 요청이 외부 API 스펙으로 매핑돼 송신
 *   2) Bearer 토큰 헤더 부착
 *   3) 외부 API 응답이 CreditScoreResult 로 정확히 매핑
 *   4) 외부 API 5xx 실패 시 예외 전파
 */
class HttpCreditScoreEngineTest {

    private static final String BASE_URL = "https://api.example.com";
    private static final String API_KEY  = "test-key";

    private MockRestServiceServer server;
    private HttpCreditScoreEngine engine;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        RestClient.Builder builder = RestClient.builder(restTemplate);
        // timeout 0 → builder 의 mock factory 유지. backoff 10ms 로 짧게 (테스트 속도).
        engine = new HttpCreditScoreEngine(builder, BASE_URL, API_KEY,
                /*connect*/ 0, /*read*/ 0, /*maxAttempts*/ 3, /*backoff*/ 10L);
    }

    @Test
    void PASS_응답_매핑() {
        server.expect(requestTo(BASE_URL + "/v1/credit-score"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(jsonPath("$.customerId").value(1001))
                .andExpect(jsonPath("$.loanType").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(30000000))
                .andExpect(jsonPath("$.period").value(36))
                .andExpect(jsonPath("$.employmentType").value("EMPLOYEE"))
                .andExpect(jsonPath("$.income").value(60000000))
                .andRespond(withSuccess("""
                        {
                          "decision":"PASS",
                          "score":740,
                          "grade":"BBB",
                          "pdBps":150,
                          "limitAmount":50000000,
                          "rejectReason":null,
                          "engineVersion":"KCB-2.4"
                        }
                        """, MediaType.APPLICATION_JSON));

        CreditScoreResult r = engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isTrue();
        assertThat(r.score()).isEqualTo(740);
        assertThat(r.grade()).isEqualTo("BBB");
        assertThat(r.pdBps()).isEqualTo(150);
        assertThat(r.estimatedLimitAmt()).isEqualTo(50_000_000L);
        assertThat(r.rejectReasonCd()).isNull();
        assertThat(r.engineVersion()).isEqualTo("KCB-2.4");
        server.verify();
    }

    @Test
    void REJECT_응답_매핑() {
        server.expect(requestTo(BASE_URL + "/v1/credit-score"))
                .andRespond(withSuccess("""
                        {
                          "decision":"REJECT",
                          "score":null, "grade":null, "pdBps":null,
                          "limitAmount":null,
                          "rejectReason":"BLACKLIST",
                          "engineVersion":"KCB-2.4"
                        }
                        """, MediaType.APPLICATION_JSON));

        CreditScoreResult r = engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isFalse();
        assertThat(r.decision()).isEqualTo("REJECT");
        assertThat(r.rejectReasonCd()).isEqualTo("BLACKLIST");
        assertThat(r.estimatedLimitAmt()).isNull();
    }

    @Test
    void apiKey_빈_문자열이면_Authorization_헤더_미부착() {
        // 새 engine 인스턴스 — api-key 미설정
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer noKeyServer = MockRestServiceServer.bindTo(restTemplate).build();
        RestClient.Builder builder = RestClient.builder(restTemplate);
        HttpCreditScoreEngine noKeyEngine = new HttpCreditScoreEngine(builder, BASE_URL, "",
                0, 0, 3, 10L);

        noKeyServer.expect(requestTo(BASE_URL + "/v1/credit-score"))
                .andExpect(MockRestRequestMatchersHeaderAbsent.headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("""
                        { "decision":"PASS", "score":700, "grade":"BBB", "pdBps":200,
                          "limitAmount":10000000, "rejectReason":null, "engineVersion":"KCB-2.4" }
                        """, MediaType.APPLICATION_JSON));

        noKeyEngine.evaluate(req(20_000_000L, 5_000_000L, "EMPLOYEE"));
        noKeyServer.verify();
    }

    @Test
    void 외부_API_5xx_재시도_3회_후_실패() {
        // 3번 모두 5xx → CreditScoreEngineException
        server.expect(ExpectedCount.times(3), requestTo(BASE_URL + "/v1/credit-score"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE")))
                .isInstanceOf(CreditScoreEngineException.class);
        server.verify();
    }

    @Test
    void 외부_API_4xx_재시도_없이_즉시_실패() {
        // 4xx 한 번만 — retry 안 함
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/v1/credit-score"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE")))
                .isInstanceOf(CreditScoreEngineException.class);
        server.verify();
    }

    @Test
    void 외부_API_5xx_2회_후_3회차_성공() {
        server.expect(ExpectedCount.times(2), requestTo(BASE_URL + "/v1/credit-score"))
                .andRespond(withServerError());
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/v1/credit-score"))
                .andRespond(withSuccess("""
                        { "decision":"PASS", "score":700, "grade":"BBB", "pdBps":200,
                          "limitAmount":10000000, "rejectReason":null, "engineVersion":"KCB-2.4" }
                        """, MediaType.APPLICATION_JSON));

        CreditScoreResult r = engine.evaluate(req(60_000_000L, 30_000_000L, "EMPLOYEE"));

        assertThat(r.isPass()).isTrue();
        assertThat(r.score()).isEqualTo(700);
        server.verify();
    }

    private CreditScoreRequest req(Long income, Long requested, String emp) {
        return new CreditScoreRequest(
                1001L, "CREDIT", requested, 36, "LIVING", emp, income);
    }

    /** Authorization 헤더 없음 검증 보조 — MockRestRequestMatchers 에 없는 헬퍼. */
    private static final class MockRestRequestMatchersHeaderAbsent {
        static org.springframework.test.web.client.RequestMatcher headerDoesNotExist(String name) {
            return request -> assertThat(request.getHeaders().containsKey(name))
                    .as("header %s should not be present", name)
                    .isFalse();
        }
    }
}
