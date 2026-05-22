package com.bank.ai.rag.ingestion.embedder;

import com.bank.ai.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * OpenAiEmbeddingClient 단위 테스트 — MockRestServiceServer 로 OpenAI 응답을 stub.
 *
 * 검증 포인트:
 *   1) /v1/embeddings POST, Bearer 토큰, model·dimensions·input 페이로드
 *   2) data[].index 순서가 뒤섞여 와도 입력 순서로 정렬
 *   3) api-key 가 빈 문자열이면 Authorization 헤더 미부착 (사내 프록시 케이스)
 *   4) 응답 차원 불일치 시 EmbeddingException
 *   5) 5xx 는 maxAttempts 만큼 재시도 후 실패
 *   6) 4xx 는 즉시 실패
 *   7) 빈 입력은 외부 호출 없이 빈 리스트 반환
 */
class OpenAiEmbeddingClientTest {

    private static final String BASE_URL = "https://api.openai.test";
    private static final String API_KEY  = "sk-test";
    private static final int    DIMENSION = 4;          // 테스트는 4차원으로 축소

    private MockRestServiceServer server;
    private OpenAiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        RestClient.Builder builder = RestClient.builder(restTemplate);
        // timeout 0 → builder 의 mock factory 보존, backoff 10ms 로 짧게
        RagProperties props = props(API_KEY, /*maxAttempts*/ 3);
        client = new OpenAiEmbeddingClient(builder, props, 0, 0);
    }

    @Test
    void 정상_응답을_입력_순서로_정렬해서_반환() {
        // data[] 순서는 일부러 뒤집어 보냄 — index 필드로 정렬되는지 확인
        server.expect(requestTo(BASE_URL + "/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("text-embedding-3-small"))
                .andExpect(jsonPath("$.dimensions").value(DIMENSION))
                .andExpect(jsonPath("$.input[0]").value("첫번째 청크"))
                .andExpect(jsonPath("$.input[1]").value("두번째 청크"))
                .andRespond(withSuccess("""
                        {
                          "object":"list",
                          "model":"text-embedding-3-small",
                          "data":[
                            {"object":"embedding","index":1,"embedding":[0.5,0.6,0.7,0.8]},
                            {"object":"embedding","index":0,"embedding":[0.1,0.2,0.3,0.4]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<float[]> vectors = client.embed(List.of("첫번째 청크", "두번째 청크"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
        assertThat(vectors.get(1)).containsExactly(0.5f, 0.6f, 0.7f, 0.8f);
        server.verify();
    }

    @Test
    void api_key_빈_문자열이면_Authorization_헤더_미부착() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer noKeyServer = MockRestServiceServer.bindTo(rt).build();
        OpenAiEmbeddingClient noKeyClient = new OpenAiEmbeddingClient(
                RestClient.builder(rt), props("", 3), 0, 0);

        noKeyServer.expect(requestTo(BASE_URL + "/v1/embeddings"))
                .andExpect(req -> assertThat(req.getHeaders().containsKey("Authorization"))
                        .as("Authorization 헤더가 없어야 함").isFalse())
                .andRespond(withSuccess("""
                        { "object":"list","model":"x","data":[
                          {"index":0,"embedding":[0.1,0.2,0.3,0.4]} ] }
                        """, MediaType.APPLICATION_JSON));

        noKeyClient.embed(List.of("청크"));
        noKeyServer.verify();
    }

    @Test
    void 응답_차원이_설정값과_다르면_예외() {
        server.expect(requestTo(BASE_URL + "/v1/embeddings"))
                .andRespond(withSuccess("""
                        { "object":"list","model":"x","data":[
                          {"index":0,"embedding":[0.1,0.2,0.3]} ] }
                        """, MediaType.APPLICATION_JSON));   // 3차원 — 설정은 4

        assertThatThrownBy(() -> client.embed(List.of("청크")))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("차원 불일치");
    }

    @Test
    void 응답_개수가_입력보다_적으면_예외() {
        server.expect(requestTo(BASE_URL + "/v1/embeddings"))
                .andRespond(withSuccess("""
                        { "object":"list","model":"x","data":[
                          {"index":0,"embedding":[0.1,0.2,0.3,0.4]} ] }
                        """, MediaType.APPLICATION_JSON));   // 1개만 응답, 입력은 2개

        assertThatThrownBy(() -> client.embed(List.of("a", "b")))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("개수 불일치");
    }

    @Test
    void 외부_API_5xx_3회_후_실패() {
        server.expect(ExpectedCount.times(3), requestTo(BASE_URL + "/v1/embeddings"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.embed(List.of("청크")))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("3회 재시도");
        server.verify();
    }

    @Test
    void 외부_API_4xx_재시도_없이_즉시_실패() {
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/v1/embeddings"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.embed(List.of("청크")))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("client error");
        server.verify();
    }

    @Test
    void 외부_API_5xx_2회_후_3회차_성공() {
        server.expect(ExpectedCount.times(2), requestTo(BASE_URL + "/v1/embeddings"))
                .andRespond(withServerError());
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/v1/embeddings"))
                .andRespond(withSuccess("""
                        { "object":"list","model":"x","data":[
                          {"index":0,"embedding":[0.1,0.2,0.3,0.4]} ] }
                        """, MediaType.APPLICATION_JSON));

        List<float[]> vectors = client.embed(List.of("청크"));

        assertThat(vectors).hasSize(1);
        assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
        server.verify();
    }

    @Test
    void 빈_입력은_외부_호출_없이_빈_리스트() {
        // server 에 expect 를 안 걸어도 호출이 없어야 verify 통과
        List<float[]> vectors = client.embed(List.of());

        assertThat(vectors).isEmpty();
        server.verify();
    }

    @Test
    void dimension_은_설정값을_반환() {
        assertThat(client.dimension()).isEqualTo(DIMENSION);
    }

    private static RagProperties props(String apiKey, int maxAttempts) {
        return new RagProperties(
                new RagProperties.Embed(
                        "openai",
                        "text-embedding-3-small",
                        DIMENSION,
                        16,
                        5000,
                        new RagProperties.Openai(
                                BASE_URL, apiKey, 0, 0, maxAttempts, /*backoff*/ 10L)
                ),
                new RagProperties.Chunk(800, 120),
                new RagProperties.Corpus("docs/corpus"),
                new RagProperties.Scheduler(false, "0 0 3 * * *")
        );
    }
}
