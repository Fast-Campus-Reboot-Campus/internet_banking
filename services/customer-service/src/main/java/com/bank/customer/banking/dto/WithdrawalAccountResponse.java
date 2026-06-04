package com.bank.customer.banking.dto;

public record WithdrawalAccountResponse(
        Long   withdrawalAccountId,
        String accountNumber,
        String bankCode,
        String bankName,
        String accountHolderName,
        String accountAlias,
        String registrationType,
        int    priorityOrder,
        String registeredAt
) {}
