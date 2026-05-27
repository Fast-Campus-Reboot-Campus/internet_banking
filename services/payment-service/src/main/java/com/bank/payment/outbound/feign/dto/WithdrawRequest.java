package com.bank.payment.outbound.feign.dto;

public record WithdrawRequest(
        String accountNo,
        Long amount,
        String currency,
        String transactionType,   // TRANSFER_OUT
        String referenceNo,       // 결제지시번호
        Counterparty counterparty,
        String memo
) {
    public record Counterparty(
            String bankCode,
            String accountNo,
            String holderName
    ) {}
}
