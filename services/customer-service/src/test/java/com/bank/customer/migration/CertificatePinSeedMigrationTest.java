package com.bank.customer.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V10 시드 인증서 PIN 백필 마이그레이션 통합테스트 (실제 PostgreSQL).
 *
 * <p>단위테스트는 Flyway를 끄고(H2 엔티티 DDL) 돌기 때문에 V10이 실행되지 않는다. 따라서
 * "V5가 cert_pin_hash 컬럼만 추가하고 V4 시드를 백필하지 않아 NULL로 남는" 부류의 회귀를
 * 단위테스트로는 잡을 수 없다. NULL 해시는 {@code CertLoginService}에서 계정 비밀번호로
 * fallback되어 6자리 PIN과 영원히 불일치 → 금융인증서 로그인 실패로 이어진다.
 *
 * <p>이 테스트는 실 Postgres에 V1~V10을 그대로 적용해 시드 인증서 3종의 PIN 해시가
 * 채워지고(NULL 아님) 데모 PIN {@code 123456}과 실제로 일치함을 검증한다. V10 SQL에
 * 하드코딩된 bcrypt 해시의 정확성까지 함께 보장한다.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CertificatePinSeedMigrationTest {

    private static final String SEEDED_PIN = "123456";
    private static final String[] SEEDED_SERIALS = {
            "COMMON-TEST-2024-000001",
            "FINCERT-TEST-2024-000001",
            "AXFUL-TEST-2024-000001"
    };

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeAll
    void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate(); // V1~V10 + beforeEachMigrate 콜백. 하나라도 실패하면 예외로 테스트 실패
    }

    @Test
    @DisplayName("V10 후 시드 인증서 3종의 cert_pin_hash가 채워지고 PIN '123456'과 일치한다 (금융인증서 로그인 복구·회귀 방지)")
    void seededCertPinsBackfilledAndMatchPin() throws Exception {
        for (String serial : SEEDED_SERIALS) {
            String hash = certPinHash(serial);
            assertThat(hash)
                    .as("%s 의 cert_pin_hash는 NULL이 아니어야 함 (NULL이면 계정 비밀번호 fallback → PIN 로그인 실패)", serial)
                    .isNotNull();
            assertThat(encoder.matches(SEEDED_PIN, hash))
                    .as("%s 의 cert_pin_hash가 PIN '123456'과 일치해야 함 (V10 하드코딩 해시 정확성)", serial)
                    .isTrue();
        }
    }

    // ── helpers ──────────────────────────────────────────────────

    /** V4가 시드한 인증서 1건의 cert_pin_hash를 조회한다. 행이 없으면 테스트 실패. */
    private String certPinHash(String serial) throws SQLException {
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement ps = c.prepareStatement(
                     "SELECT cert_pin_hash FROM certificate " +
                     "WHERE certificate_serial_number = ? AND deleted_at IS NULL")) {
            ps.setString(1, serial);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("시드 인증서 %s 행 존재", serial).isTrue();
                return rs.getString(1);
            }
        }
    }
}
