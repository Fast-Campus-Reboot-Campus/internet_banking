package com.bank.deposit.dto.request;

import jakarta.validation.constraints.NotNull;

public record CommonTermsConsentRequest(
        @NotNull Long termsTemplateId,
        @NotNull Long termsVersionId,
        @NotNull Long customerId,
        String agreedYn,
        String agreedAt,
        String consentMethodCd,
        String consentTool,
        String signedDocUrl,
        String signedDocHash,
        String clientIp,
        String retentionUntil
) {}
