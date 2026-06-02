package com.bank.loan.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * advisory 마이그레이션(db/advisory-migration) 전용 Flyway.
 *
 * advisory 의 V20~V27 은 loan 의 db/migration V20~V27 과 버전 번호가 통째로 겹친다.
 * 둘을 한 Flyway 이력(flyway_schema_history)에 넣으면 "더 이상 한 버전에 여러 마이그레이션" 으로
 * 부팅이 막히므로, advisory 는 같은 loan_db 위에서 전용 이력 테이블로 분리 실행한다.
 *
 * Flyway 타입 빈으로 노출하지 않는다 — 노출하면 Boot 기본 Flyway 자동설정이 backoff 된다.
 * loan 기본 Flyway(flywayInitializer) 이후 실행되도록 @DependsOn 으로 순서를 보장하고,
 * 기본(loan_db) EntityManagerFactory 가 본 빈에 의존해 스키마 생성 후 EMF 가 뜬다(PrimaryDataSourceConfig).
 */
@Configuration
public class AdvisoryFlywayConfig {

    @Bean
    @DependsOn("flywayInitializer")
    public AdvisoryFlywayInitialized advisoryFlywayInitializer(
            @Qualifier("dataSource") DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/advisory-migration")
                .table("advisory_flyway_schema_history")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        return new AdvisoryFlywayInitialized();
    }

    /** advisory Flyway 마이그레이션 완료 마커. */
    static final class AdvisoryFlywayInitialized {
    }
}
