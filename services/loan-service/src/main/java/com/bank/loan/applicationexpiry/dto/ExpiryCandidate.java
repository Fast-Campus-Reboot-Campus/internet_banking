package com.bank.loan.applicationexpiry.dto;

import java.time.OffsetDateTime;

/**
 * 만료 후보 프로젝션.
 * validityDays: 상품별 applicationValidityDays, null 인 경우 DB coalesce 14 적용.
 */
public record ExpiryCandidate(Long applId, OffsetDateTime approvedAt, int validityDays) {}
