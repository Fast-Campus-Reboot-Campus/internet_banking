package com.bank.payment.api.dto;

import java.time.LocalDateTime;

/**
 * 결제 응답. 자행 동기 완결 → 200 OK + status=COMPLETED.
 */
public record PaymentResponse(
        String paymentInstructionId,
        String transactionNo,
        String status,
        LocalDateTime completedAt
) {}
