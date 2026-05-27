package com.bank.payment.outbound.feign.dto;

public record AccountInquiryData(
        String accountNo,
        String accountType,     // SAVINGS / DEMAND / TIME / SUBSCRIPTION
        String accountStatus,   // ACTIVE / FROZEN / CLOSED / DORMANT
        String productCode,
        String openedAt,
        String closedAt,
        String branchCode,
        Boolean fraudFlag,
        Integer version
) {}
