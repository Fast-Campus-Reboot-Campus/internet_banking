package com.bank.loan.creditreport.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminCreditInfoReportListResponse(
        List<CreditInfoReportResponse> items,
        long totalCount,
        int page,
        int size
) {
    public static AdminCreditInfoReportListResponse of(Page<CreditInfoReportResponse> p) {
        return new AdminCreditInfoReportListResponse(
                p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
    }
}
