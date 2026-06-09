package com.bank.customer.customer.dto;

import java.time.OffsetDateTime;

/**
 * 직원용 고객 목록·검색 행(行). 모든 list 화면(고객 조회·회원 상태 관리 등)의 진입점.
 *
 * <p>이름({@code partyName})은 party 도메인에 있어 Customer ⨝ Party 조인으로 채운다.
 * 주민등록번호는 암호화 저장이라 목록에서 제외하고, 복호화·마스킹이 필요한 상세 조회로 분리한다
 * (PII 노출 최소화). phone·email 마스킹은 표시 계층(프론트)에서 처리한다.
 */
public record CustomerSummaryResponse(
        Long           customerId,
        Long           partyId,
        String         partyName,
        String         phone,
        String         email,
        String         customerGradeCode,
        String         customerStatusCode,
        OffsetDateTime joinedAt,
        OffsetDateTime lastTransactionAt
) {
}
