package com.bank.loan.autodebit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * payment-service → loan-service CLEARING 완결 콜백 요청.
 *
 * payment-service가 KFTC/BOK 정산 완료 후 호출한다.
 * idempotencyKey 형식: "AUTO-{cntrId}-{rschId}-{baseDate}"
 */
public record AutoDebitPaymentResultRequest(

        @NotBlank String piId,

        /** 자동이체 배치 멱등키. payment-service가 저장해둔 값을 그대로 반환. */
        @NotBlank String idempotencyKey,

        /** COMPLETED | FAILED */
        @NotBlank @Pattern(regexp = "COMPLETED|FAILED") String status,

        String failureCategory
) {}
