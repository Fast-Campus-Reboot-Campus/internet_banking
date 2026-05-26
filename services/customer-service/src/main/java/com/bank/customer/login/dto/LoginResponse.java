package com.bank.customer.login.dto;

public record LoginResponse(
        Long customerId,
        String accessToken,
        String refreshToken
) {}
