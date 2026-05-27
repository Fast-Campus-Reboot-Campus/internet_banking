package com.bank.loan.support;

import com.bank.common.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

    static {
        POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");
        POSTGRES.start();

        REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        REDIS.start();

        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
        KAFKA.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());

        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        Path storage = Paths.get(System.getProperty("java.io.tmpdir"), "loan-test-docs");
        r.add("loan.document.storage-dir", storage::toString);
    }

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtProvider jwtProvider;

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper om;

    @BeforeAll
    void initTestAuth() {
        String token = jwtProvider.generateAccessToken(
                1L, "test@bank.com",
                List.of("ROLE_STAFF", "ROLE_OPS", "ROLE_SENIOR_REVIEWER", "ROLE_INTERNAL")
        );
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .build();
    }

    protected JsonNode extractData(MvcResult result) throws Exception {
        return om.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
