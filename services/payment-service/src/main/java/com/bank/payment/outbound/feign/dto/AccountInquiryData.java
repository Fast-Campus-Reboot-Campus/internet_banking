package com.bank.payment.outbound.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountInquiryData(
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
