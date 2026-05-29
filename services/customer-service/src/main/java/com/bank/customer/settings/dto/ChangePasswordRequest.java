package com.bank.customer.settings.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}
