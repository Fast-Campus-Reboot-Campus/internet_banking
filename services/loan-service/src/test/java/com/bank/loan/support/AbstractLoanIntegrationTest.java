package com.bank.loan.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;


/**
 * 통합 테스트 베이스. 컨테이너(Postgres / Redis) 와 MockMvc / ObjectMapper 를 공유한다.
 *
 * 싱글톤 컨테이너 패턴 — @Testcontainers / @Container 미사용.
 * 컨테이너 lifecycle 을 JVM 전체에 맞춰 두어, 여러 테스트 클래스가 캐시된 Spring 컨텍스트를
 * 재사용해도 동일한 컨테이너에 계속 접속할 수 있게 한다.
 * Ryuk 데몬이 JVM 종료 시 자동 정리한다.
 *
 *  - JPA ddl-auto = create-drop (Spring 컨텍스트 초기화 시점에 스키마 신규 생성)
 *  - 서류 스토리지 = OS 임시 디렉터리
 *  - MockMvc 기본 요청에 전 역할 JWT 포함 — 테스트용 슈퍼 토큰
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractLoanIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;
    static final KafkaContainer KAFKA;
    protected static final WireMockServer DOC_AGENT_MOCK;
    protected static final WireMockServer AUTO_REVIEW_MOCK;
    protected static final WireMockServer ADVISORY_MOCK;
    protected static final WireMockServer PAYMENT_MOCK;

    static {
        POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");
        POSTGRES.start();

        REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        REDIS.start();

        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
        KAFKA.start();

        DOC_AGENT_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        DOC_AGENT_MOCK.start();

        AUTO_REVIEW_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        AUTO_REVIEW_MOCK.start();
        AUTO_REVIEW_MOCK.stubFor(WireMock.post(WireMock.urlEqualTo("/api/ai/auto-review/evaluate"))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"track\":\"TRACK_3\",\"pd\":0.120000,\"rationale\":\"통합테스트 기본 stub\"}")));

        ADVISORY_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        ADVISORY_MOCK.start();
        ADVISORY_MOCK.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/advisory/reports"))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // 기본 stub: POST /api/v1/payments → COMPLETED
        // 개별 테스트에서 priority=1 스텁으로 특정 X-Idempotency-Key 에 대해 FAILED 등을 오버라이드 가능
        PAYMENT_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        PAYMENT_MOCK.start();
        PAYMENT_MOCK.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/payments"))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentInstructionId\":\"PI-TEST-001\"," +
                                  "\"transactionNo\":\"TXN-TEST-001\"," +
                                  "\"status\":\"COMPLETED\"," +
                                  "\"failureCategory\":null}")));
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // common_db 는 같은 테스트 컨테이너를 공유한다. common Flyway 는 전용 이력 테이블을 쓰므로
        // loan Flyway 와 충돌하지 않는다(CommonDataSourceConfig 참고).
        r.add("common.datasource.url",      POSTGRES::getJdbcUrl);
        r.add("common.datasource.username", POSTGRES::getUsername);
        r.add("common.datasource.password", POSTGRES::getPassword);

        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());

        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        r.add("loan.review.bias-check.enabled", () -> "false");
        r.add("doc-agent.base-url", () -> "http://localhost:" + DOC_AGENT_MOCK.port());
        r.add("auto-review.base-url", () -> "http://localhost:" + AUTO_REVIEW_MOCK.port());
        r.add("advisory.service.base-url", () -> "http://localhost:" + ADVISORY_MOCK.port());
        r.add("payment.url", () -> "http://localhost:" + PAYMENT_MOCK.port());
    }

    @Autowired private WebApplicationContext wac;

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper om;

    @BeforeAll
    void initTestAuth() {
        mockMvc = buildAuthMockMvc();
    }

    @BeforeEach
    void resetTestAuth() {
        mockMvc = buildAuthMockMvc();
    }

    private MockMvc buildAuthMockMvc() {
        // GatewayHeaderAuthFilter 가 X-User-Id / X-User-Role 헤더를 읽어 SecurityContext 를 설정한다.
        String roles = "ROLE_STAFF,ROLE_OPS,ROLE_SENIOR_REVIEWER,ROLE_INTERNAL,"
                + "ROLE_TELLER,ROLE_DEPUTY_MANAGER,ROLE_BRANCH_MANAGER,"
                + "ROLE_HQ_REVIEWER,ROLE_COMPLIANCE,ROLE_ADMIN";
        return MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .with(request -> {
                            request.addHeader("X-User-Id", "1");
                            request.addHeader("X-User-Role", roles);
                            return request;
                        }))
                .build();
    }

    protected JsonNode extractData(MvcResult result) throws Exception {
        return om.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
