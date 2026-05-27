package com.bank.loan.creditreport.dto;

import java.util.List;

public record CreditInfoReportListResponse(
        Long cntrId,
        int totalCount,
        List<CreditInfoReportResponse> items
) {
    public static CreditInfoReportListResponse of(Long cntrId, List<CreditInfoReportResponse> items) {
        return new CreditInfoReportListResponse(cntrId, items.size(), items);
    }
}
