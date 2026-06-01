package com.bank.customer.cert.dto;

import java.time.OffsetDateTime;

public record QrGenerateResponse(
        String tokenHash,
        String confirmCode,
        OffsetDateTime expiryAt
) {}
