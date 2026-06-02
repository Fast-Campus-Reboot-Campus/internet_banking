package com.bank.customer.cert.dto;

public record QrStatusResponse(
        String status,
        Long customerId,
        String accessToken,
        String refreshToken
) {}
