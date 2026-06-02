package com.bank.customer.cert.dto;

public record CertIssueResponse(
        String serialNumber,
        String certType,
        String issuerName,
        String subjectDn,
        String issuedDate,
        String expiryDate
) {}
