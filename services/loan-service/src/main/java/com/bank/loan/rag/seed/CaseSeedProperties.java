package com.bank.loan.rag.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 합성 케이스 seed 설정 — Phase E (E3-8).
 *
 * <p>{@code ai.case-seed.enabled=true} 시 기동 시점에 {@link SyntheticCaseSeedLoader} 가
 * {@code loan_review_outbox} 에 합성 케이스를 적재. 기본 off — 개발 환경 최초 1회 실행용.
 *
 * @param enabled      seed 활성 여부
 * @param count        생성할 합성 케이스 수 (기본 10,000)
 * @param batchSize    JDBC batch insert 단위 (기본 500)
 * @param aggregateBase seed aggregate_id 시작값 (실제 rev_id 충돌 방지용 고정 오프셋)
 */
@ConfigurationProperties(prefix = "ai.case-seed")
public record CaseSeedProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10000") int count,
        @DefaultValue("500") int batchSize,
        @DefaultValue("9000000") long aggregateBase
) {}
