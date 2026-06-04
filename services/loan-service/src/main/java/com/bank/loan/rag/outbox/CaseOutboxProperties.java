package com.bank.loan.rag.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 유사 케이스 outbox 파이프라인 설정 — Phase E (E3-4).
 *
 * <p>기존 HTTP 푸시({@code ai.similar-case-export})와 독립된 플래그. 둘 중 하나만 켜서 사용.
 *
 * @param enabled        outbox 파이프라인(필러+디스패처) 활성화
 * @param lookbackDays   필러 증분 조회 기준 일수
 * @param fillIntervalMs 필러 주기(ms)
 * @param pollIntervalMs 디스패처 주기(ms)
 * @param batchSize      디스패처 1회 발행 최대 건수
 * @param topic          Kafka 발행 토픽
 */
@ConfigurationProperties(prefix = "ai.case-outbox")
public record CaseOutboxProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("1") int lookbackDays,
        @DefaultValue("60000") long fillIntervalMs,
        @DefaultValue("5000") long pollIntervalMs,
        @DefaultValue("200") int batchSize,
        @DefaultValue("loan-review.case-indexed.v1") String topic
) {}
