package com.bank.customer.settings.dto;

public record UpdateNotificationRequest(
        boolean smsReceiveYn,
        boolean emailReceiveYn,
        boolean postalReceiveYn
) {}
