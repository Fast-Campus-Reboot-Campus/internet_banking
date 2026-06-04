package com.bank.customer.party.dto;

import com.bank.customer.party.domain.TaxResidencyInfo;

public record TaxResidencyResponse(
        Long    taxResidencyId,
        String  residentTypeCode,
        String  taxCountryCode,
        String  foreignTin,
        Integer withholdingRateBps,
        String  taxResidencyConfirmDate
) {
    public static TaxResidencyResponse from(TaxResidencyInfo t) {
        return new TaxResidencyResponse(
                t.getTaxResidencyId(), t.getResidentTypeCode(),
                t.getTaxCountryCode(), t.getForeignTin(),
                t.getWithholdingRateBps(), t.getTaxResidencyConfirmDate());
    }
}
