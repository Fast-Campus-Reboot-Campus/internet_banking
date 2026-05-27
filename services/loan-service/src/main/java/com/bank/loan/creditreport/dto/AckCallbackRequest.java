package com.bank.loan.creditreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 외부 신용평가기관 ACK callback 페이로드.
 *
 *   externalAckNo  외부 기관이 부여한 ACK 추적 번호 (감사용)
 *   ackedAt        외부 기관 기준 ACK 시각
 *
 * 인증·서명검증은 본 단계 외 — 11 plan 의 보안 강화에서 처리.
 */
public record AckCallbackRequest(
        @NotBlank @Size(max = 100) String externalAckNo,
        @NotNull OffsetDateTime ackedAt
) {
}
