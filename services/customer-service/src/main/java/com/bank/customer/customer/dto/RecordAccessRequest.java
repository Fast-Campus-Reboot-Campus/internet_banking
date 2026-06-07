package com.bank.customer.customer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 명시적 접근 기록 요청(연락처 등 민감정보 열람). 행위 직원은 X-Employee-Id 헤더로 식별한다.
 */
public record RecordAccessRequest(
        @NotBlank String actionCode,
        String reason) {
}
