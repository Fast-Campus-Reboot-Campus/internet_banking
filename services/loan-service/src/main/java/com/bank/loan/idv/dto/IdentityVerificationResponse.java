package com.bank.loan.idv.dto;

import com.bank.loan.idv.domain.LoanIdentityVerification;

import java.time.OffsetDateTime;

public record IdentityVerificationResponse(
        Long idvId,
        Long applId,
        Long customerId,
        String idvMethodCd,
        String idvStatusCd,
        String idvResultCd,
        String idvTargetCd,
        String ciHash,
        String diHash,
        String mobileNoMasked,
        OffsetDateTime verifiedAt,
        String externalTxNo
) {
    public static IdentityVerificationResponse of(LoanIdentityVerification v) {
        return new IdentityVerificationResponse(
                v.getIdvId(), v.getApplId(), v.getCustomerId(),
                v.getIdvMethodCd(), v.getIdvStatusCd(), v.getIdvResultCd(), v.getIdvTargetCd(),
                v.getCiHash(), v.getDiHash(), v.getMobileNoMasked(),
                v.getVerifiedAt(), v.getExternalTxNo()
        );
    }
}
