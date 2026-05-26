package com.bank.loan.guarantor.dto;

import java.util.List;

public record GuarantorAgreementListResponse(
        Long applId,
        int count,
        List<GuarantorAgreementResponse> items
) {
    public static GuarantorAgreementListResponse of(Long applId, List<GuarantorAgreementResponse> items) {
        return new GuarantorAgreementListResponse(applId, items.size(), items);
    }
}
