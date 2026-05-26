package com.bank.loan.certificate.dto;

import com.bank.loan.certificate.domain.LoanCertificate;

import java.time.OffsetDateTime;

public record LoanCertificateResponse(
        Long certId,
        Long cntrId,
        Long customerId,
        String certTypeCd,
        String certNo,
        String certStatusCd,
        String certPurposeCd,
        String certDocUrl,
        String certDocHash,
        String issueChannelCd,
        OffsetDateTime issuedAt,
        String retentionUntil
) {
    public static LoanCertificateResponse of(LoanCertificate c) {
        return new LoanCertificateResponse(
                c.getCertId(), c.getCntrId(), c.getCustomerId(),
                c.getCertTypeCd(), c.getCertNo(), c.getCertStatusCd(),
                c.getCertPurposeCd(),
                c.getCertDocUrl(), c.getCertDocHash(),
                c.getIssueChannelCd(),
                c.getIssuedAt(), c.getRetentionUntil()
        );
    }
}
