package com.bank.customer.cert.dto;

public record CertSummaryResponse(
        String serialNumber,
        String certType,
        String certTypeName,
        String issuerName,
        String issuedDate,
        String expiryDate,
        String status,
        String statusName
) {}
