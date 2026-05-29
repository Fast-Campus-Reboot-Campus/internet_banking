package com.bank.customer.settings.dto;

public record SettingsResponse(
        String name,
        String email,
        String phone,
        String zipCode,
        String address,
        String addressDetail,
        boolean smsReceiveYn,
        boolean emailReceiveYn,
        boolean postalReceiveYn
) {}
