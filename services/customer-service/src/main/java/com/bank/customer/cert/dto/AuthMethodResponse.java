package com.bank.customer.cert.dto;

import com.bank.customer.cert.domain.AuthMethod;

import java.time.OffsetDateTime;

public record AuthMethodResponse(
        Long           authMethodId,
        String         authMethodTypeCode,
        String         authMethodAliasName,
        String         authMethodStatusCode,
        boolean        primary,
        String         authMethodRegisteredDate,
        String         authMethodExpiryDate,
        OffsetDateTime lastUsedAt
) {
    public static AuthMethodResponse from(AuthMethod m) {
        return new AuthMethodResponse(
                m.getAuthMethodId(), m.getAuthMethodTypeCode(), m.getAuthMethodAliasName(),
                m.getAuthMethodStatusCode(), m.isPrimary(),
                m.getAuthMethodRegisteredDate(), m.getAuthMethodExpiryDate(),
                m.getAuthMethodLastUsedAt());
    }
}
