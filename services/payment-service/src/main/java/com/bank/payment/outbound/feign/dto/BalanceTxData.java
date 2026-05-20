package com.bank.payment.outbound.feign.dto;

public record BalanceTxData(
        String depositTransactionNo,
        String accountNo,
        Long amount,
        Long balanceBefore,
        Long balanceAfter,
        String transactionAt,
        String transactionType  // TRANSFER_OUT / TRANSFER_IN / FEE
) {}
