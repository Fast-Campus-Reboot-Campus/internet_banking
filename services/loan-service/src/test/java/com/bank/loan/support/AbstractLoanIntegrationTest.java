package com.bank.loan.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.nio.file.Paths;

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
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractLoanIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;
    static final KafkaContainer KAFKA;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
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

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper om;

    protected JsonNode extractData(MvcResult result) throws Exception {
        return om.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
