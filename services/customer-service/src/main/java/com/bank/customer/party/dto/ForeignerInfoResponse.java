package com.bank.customer.party.dto;

import com.bank.customer.party.domain.ForeignerInfo;

public record ForeignerInfoResponse(
        String passportNo,
        String passportCountryCode,
        String passportExpiryDate,
        String stayQualificationCode,
        String stayExpiryDate,
        String recentEntryDate,
        String stayAddress
) {
    public static ForeignerInfoResponse from(ForeignerInfo f) {
        return new ForeignerInfoResponse(
                f.getPassportNo(), f.getPassportCountryCode(), f.getPassportExpiryDate(),
                f.getStayQualificationCode(), f.getStayExpiryDate(),
                f.getRecentEntryDate(), f.getStayAddress());
    }
}
