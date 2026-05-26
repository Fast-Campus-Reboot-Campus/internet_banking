package com.bank.loan.certificate.dto;

import java.util.List;

public record LoanCertificateListResponse(
        Long cntrId,
        int totalCount,
        List<LoanCertificateResponse> items
) {
    public static LoanCertificateListResponse of(Long cntrId, List<LoanCertificateResponse> items) {
        return new LoanCertificateListResponse(cntrId, items.size(), items);
    }
}
