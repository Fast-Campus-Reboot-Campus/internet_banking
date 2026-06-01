package com.bank.customer.cert.dto;

public record CertDetailResponse(
        String serialNumber,
        String certType,
        String certTypeName,
        String issuerName,
        String subjectDn,
        String issuerDn,
        String issuedDate,
        String expiryDate,
        String status,
        String statusName,
        String purposeCode,
        boolean hasPinSet
) {}
