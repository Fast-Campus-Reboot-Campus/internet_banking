package com.bank.loan.schedule.dto;

import java.util.List;

public record RepaymentScheduleListResponse(
        Long cntrId,
        String rschVersionCd,
        int totalCount,
        Long totalScheduledPrincipal,
        Long totalScheduledInterest,
        Long totalScheduledAmount,
        List<RepaymentScheduleResponse> items
) {
    public static RepaymentScheduleListResponse of(Long cntrId, String version, List<RepaymentScheduleResponse> items) {
        long totalP = items.stream().mapToLong(RepaymentScheduleResponse::scheduledPrincipal).sum();
        long totalI = items.stream().mapToLong(RepaymentScheduleResponse::scheduledInterest).sum();
        long totalT = items.stream().mapToLong(RepaymentScheduleResponse::scheduledTotal).sum();
        return new RepaymentScheduleListResponse(cntrId, version, items.size(), totalP, totalI, totalT, items);
    }
}
