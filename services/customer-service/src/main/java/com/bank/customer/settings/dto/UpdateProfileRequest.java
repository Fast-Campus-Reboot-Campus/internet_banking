package com.bank.customer.settings.dto;

public record UpdateProfileRequest(
        String email,
        String phone,
        String zipCode,
        String address,
        String addressDetail
) {}
