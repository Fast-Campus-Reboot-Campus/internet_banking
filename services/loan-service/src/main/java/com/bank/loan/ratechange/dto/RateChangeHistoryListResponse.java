package com.bank.loan.ratechange.dto;

import java.util.List;

public record RateChangeHistoryListResponse(
        Long cntrId,
        int totalCount,
        List<RateChangeHistoryResponse> items
) {
    public static RateChangeHistoryListResponse of(Long cntrId, List<RateChangeHistoryResponse> items) {
        return new RateChangeHistoryListResponse(cntrId, items.size(), items);
    }
}
