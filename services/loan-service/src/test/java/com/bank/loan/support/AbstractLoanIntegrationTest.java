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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 통합 테스트 베이스. 컨테이너(Postgres / Redis) 와 MockMvc / ObjectMapper 를 공유.
 *
 *  - JPA ddl-auto = create-drop (테스트 컨테이너 부팅 시 신규 스키마 생성)
 *  - 서류 스토리지 = OS 임시 디렉터리
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractLoanIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        // @DynamicPropertySource 평가 전에 컨테이너가 기동되어 있어야 getJdbcUrl 등이 동작한다.
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());

        Path storage = Paths.get(System.getProperty("java.io.tmpdir"), "loan-test-docs");
        r.add("loan.document.storage-dir", storage::toString);
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper om;

    protected JsonNode extractData(MvcResult result) throws Exception {
        return om.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
