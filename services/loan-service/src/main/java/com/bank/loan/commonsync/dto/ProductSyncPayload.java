package com.bank.loan.commonsync.dto;

/**
 * PRODUCT 타입 outbox payload.
 * LoanProduct 스냅샷 → CommonProduct 생성에 필요한 필드.
 */
public record ProductSyncPayload(
        String prodCd,
        String bizDivCd,
        String prodName,
        String loanTypeCd,
        String targetCustomerCd,
        Long minAmount,
        Long maxAmount,
        Integer minPeriodMo,
        Integer maxPeriodMo,
        String saleStartDate,
        String saleEndDate,
        String prodStatus
) {}
