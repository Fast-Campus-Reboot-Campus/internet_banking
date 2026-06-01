package com.bank.loan.payment.client.dto;

public record PaymentResponse(
        String paymentInstructionId,
        String transactionNo,
        String status,
        String failureCategory
) {
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED    = "FAILED";
    public static final String STATUS_CLEARING  = "CLEARING";
}
