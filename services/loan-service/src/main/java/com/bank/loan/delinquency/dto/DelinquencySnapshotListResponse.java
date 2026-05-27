package com.bank.loan.delinquency.dto;

import java.util.List;

public record DelinquencySnapshotListResponse(
        Long cntrId,
        Long dlqId,
        int totalCount,
        List<DelinquencySnapshotResponse> items
) {
    public static DelinquencySnapshotListResponse of(Long cntrId, Long dlqId, List<DelinquencySnapshotResponse> items) {
        return new DelinquencySnapshotListResponse(cntrId, dlqId, items.size(), items);
    }
}
