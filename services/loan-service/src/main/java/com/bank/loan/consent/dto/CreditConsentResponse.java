package com.bank.loan.consent.dto;

import com.bank.loan.consent.domain.CreditConsent;

import java.time.OffsetDateTime;

public record CreditConsentResponse(
        Long csntId,
        Long applId,
        Long customerId,
        String consentTypeCd,
        String consentScopeCd,
        String consentTargetCd,
        String consentYn,
        OffsetDateTime consentedAt,
        String consentMethodCd,
        String signedDocUrl,
        String retentionUntil,
        String withdrawnYn,
        OffsetDateTime withdrawnAt
) {
    public static CreditConsentResponse of(CreditConsent c) {
        return new CreditConsentResponse(
                c.getCsntId(), c.getApplId(), c.getCustomerId(),
                c.getConsentTypeCd(), c.getConsentScopeCd(), c.getConsentTargetCd(),
                c.getConsentYn(), c.getConsentedAt(),
                c.getConsentMethodCd(), c.getSignedDocUrl(), c.getRetentionUntil(),
                c.getWithdrawnYn(), c.getWithdrawnAt()
        );
    }
}
