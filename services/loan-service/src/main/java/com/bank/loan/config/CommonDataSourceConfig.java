package com.bank.loan.config;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 공통 계좌 DB(common_db) datasource·JPA 구성.
 *
 * Phase 1: loan-service 가 common_db 를 채택해 공통 계좌 마스터(common_account)를 직접 다룬다.
 * 엔티티/리포지토리는 {@code com.bank.commonaccount.*} 패키지로 분리해 기본(loan_db) EMF 스캔
 * 범위({@code com.bank.loan}, {@code com.bank.common}) 와 겹치지 않게 한다.
 *
 * common_db 전용 Flyway 는 Flyway 타입 빈으로 노출하지 않는다 — 노출하면 Boot 의 기본 Flyway
 * 자동설정(@ConditionalOnMissingBean(Flyway))이 backoff 되어 loan_db 마이그레이션이 멈춘다.
 * 대신 초기화 마커 빈에서 직접 migrate() 를 호출한다.
 */
@Configuration
@EnableJpaRepositories(
        // 루트 스캔 — common_account 외 공통 부모(product/contract/transaction) 서브패키지 리포지토리 포함.
        // com.bank.loan / com.bank.common (Primary EMF 스캔) 과 겹치지 않음.
        basePackages = "com.bank.commonaccount",
        entityManagerFactoryRef = "commonEntityManagerFactory",
        transactionManagerRef = "commonTransactionManager"
)
public class CommonDataSourceConfig {

    @Bean
    @ConfigurationProperties("common.datasource")
    public DataSourceProperties commonDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource commonDataSource(
            @Qualifier("commonDataSourceProperties") DataSourceProperties commonDataSourceProperties) {
        return commonDataSourceProperties.initializeDataSourceBuilder().build();
    }

    /**
     * common_db Flyway 마이그레이션 실행. Flyway 타입을 노출하지 않으려 마커를 반환한다.
     * commonEntityManagerFactory 가 본 빈에 @DependsOn 으로 의존해 스키마 생성 후 EMF 가 뜬다.
     */
    @Bean
    public CommonFlywayInitialized commonFlywayInitializer(
            @Qualifier("commonDataSource") DataSource commonDataSource) {
        Flyway.configure()
                .dataSource(commonDataSource)
                .locations("classpath:db/common-migration")
                // 전용 이력 테이블 — 같은 물리 DB 를 공유하는 통합 테스트에서 loan Flyway 와 충돌 방지
                .table("common_flyway_schema_history")
                // baselineOnMigrate: 이력 테이블이 없는 기존 DB 에서 첫 마이그레이션 허용.
                // baselineVersion("0"): 기본값 "1" 이면 V1 이 베이스라인으로 건너뛰어져
                // V1(common_account DDL) 이 실행되지 않은 채 V2(seed) 가 실패한다.
                // "0" 으로 설정해 V1 부터 모두 실행되도록 한다.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
        return new CommonFlywayInitialized();
    }

    @Bean
    @DependsOn("commonFlywayInitializer")
    public LocalContainerEntityManagerFactoryBean commonEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("commonDataSource") DataSource commonDataSource) {
        return builder.dataSource(commonDataSource)
                // 루트 스캔 — .domain(account) 외 product/contract/transaction 서브패키지 엔티티 포함.
                .packages("com.bank.commonaccount")
                .persistenceUnit("common")
                .build();
    }

    @Bean
    public PlatformTransactionManager commonTransactionManager(
            @Qualifier("commonEntityManagerFactory") EntityManagerFactory commonEntityManagerFactory) {
        return new JpaTransactionManager(commonEntityManagerFactory);
    }

    /** common_db Flyway 마이그레이션 완료 마커. */
    static final class CommonFlywayInitialized {
    }
}
