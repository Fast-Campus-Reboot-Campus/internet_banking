package com.bank.customer.cert.dto;

public record QrCertStatusResponse(
        String status,
        String serialNumber,
        String issuedDate,
        String expiryDate
) {}
