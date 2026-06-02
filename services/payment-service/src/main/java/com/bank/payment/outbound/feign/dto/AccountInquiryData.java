package com.bank.payment.outbound.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountInquiryData(
        Long accountId,                               // deposit PK — by-number 응답 직접 필드명
        @JsonProperty("accountNumber") String accountNo,
        String accountType,
        String accountStatus,   // ACTIVE / DORMANT / SUSPENDED / CLOSED (deposit 실제 enum)
        String productCode,
        String openedAt,
        String closedAt,
        String branchCode,
        Boolean fraudFlag,      // deposit 미제공 → null. Boolean.TRUE.equals(null)=false 로 안전.
        Integer version
) {}
