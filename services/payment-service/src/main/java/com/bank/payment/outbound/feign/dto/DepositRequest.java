package com.bank.payment.outbound.feign.dto;

public record DepositRequest(
        String accountNo,
        Long amount,
        String currency,
        String transactionType,   // TRANSFER_IN
        String referenceNo,
        Counterparty counterparty,
        String memo
) {
    public record Counterparty(
            String bankCode,
            String accountNo,
            String holderName,
            String passbookDisplay  // P-027 송신자 표시명
    ) {}
}
