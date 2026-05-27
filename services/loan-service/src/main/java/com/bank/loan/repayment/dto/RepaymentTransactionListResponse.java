package com.bank.loan.repayment.dto;

import java.util.List;

public record RepaymentTransactionListResponse(
        Long cntrId,
        int totalCount,
        Long totalPaidAmount,
        List<RepaymentTransactionResponse> items
) {
    public static RepaymentTransactionListResponse of(Long cntrId, List<RepaymentTransactionResponse> items) {
        long sum = items.stream().mapToLong(RepaymentTransactionResponse::totalAmount).sum();
        return new RepaymentTransactionListResponse(cntrId, items.size(), sum, items);
    }
}
