package com.bank.loan.statushistory.dto;

import java.util.List;

public record StatusHistoryListResponse(
        String targetDomainCd,
        String targetTableCd,
        Long targetId,
        int count,
        List<StatusHistoryResponse> items
) {
    public static StatusHistoryListResponse of(String targetDomainCd, String targetTableCd,
                                               Long targetId, List<StatusHistoryResponse> items) {
        return new StatusHistoryListResponse(targetDomainCd, targetTableCd, targetId,
                items.size(), items);
    }
}
