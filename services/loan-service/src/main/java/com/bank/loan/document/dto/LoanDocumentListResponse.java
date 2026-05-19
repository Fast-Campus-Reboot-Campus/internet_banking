package com.bank.loan.document.dto;

import java.util.List;

public record LoanDocumentListResponse(
        List<LoanDocumentResponse> items,
        int totalCount
) {
    public static LoanDocumentListResponse of(List<LoanDocumentResponse> items) {
        return new LoanDocumentListResponse(items, items.size());
    }
}
