package com.bank.loan.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 유사 케이스 코퍼스 내보내기 설정 — D3-2.
 *
 * @param enabled        내보내기 활성화 여부 (기본 false)
 * @param autoReviewUrl  auto-loan-review 서비스 base URL
 * @param internalToken  X-Internal-Token 헤더 값
 * @param batchSize      1회 내보내기 최대 건수
 * @param lookbackDays   증분 조회 기준 일수 (1 = 전일)
 */
@ConfigurationProperties(prefix = "ai.similar-case-export")
public record SimilarCaseExportProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("http://localhost:8086") String autoReviewUrl,
        @DefaultValue("") String internalToken,
        @DefaultValue("200") int batchSize,
        @DefaultValue("1") int lookbackDays
) {}
