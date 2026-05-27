package com.bank.loan.guarantor.dto;

import com.bank.loan.guarantor.domain.GuarantorAgreement;
import com.bank.loan.guarantor.domain.GuarantorMaster;

import java.time.OffsetDateTime;

public record GuarantorAgreementResponse(
        Long gagrId,
        Long applId,
        Long gmstId,
        String guarantorNameMasked,
        String mobileNoMasked,
        String relationTypeCd,
        String gagrTypeCd,
        Long guaranteeAmount,
        Integer guaranteeRatioBps,
        String gagrStatusCd,
        OffsetDateTime consentedAt,
        String signedDocUrl,
        String signedDocHash
) {
    public static GuarantorAgreementResponse of(GuarantorAgreement a, GuarantorMaster m) {
        return new GuarantorAgreementResponse(
                a.getGagrId(), a.getApplId(), a.getGmstId(),
                m.getGuarantorNameMasked(), m.getMobileNoMasked(), m.getRelationTypeCd(),
                a.getGagrTypeCd(), a.getGuaranteeAmount(), a.getGuaranteeRatioBps(),
                a.getGagrStatusCd(), a.getConsentedAt(),
                a.getSignedDocUrl(), a.getSignedDocHash()
        );
    }
}
