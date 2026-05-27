package com.bank.loan.repayment.dto;

import com.bank.loan.repayment.domain.RepaymentTransaction;

import java.time.OffsetDateTime;

public record RepaymentTransactionResponse(
        Long rtxId,
        Long cntrId,
        Long rschId,
        String rtxTypeCd,
        String rtxStatusCd,
        Long totalAmount,
        Long principalAmount,
        Long interestAmount,
        Long overdueInterestAmount,
        Long feeAmount,
        String channelCd,
        String currencyCd,
        OffsetDateTime paidAt,
        String valueDate,
        Long balanceAfter,
        String idempotencyKey
) {
    public static RepaymentTransactionResponse of(RepaymentTransaction t) {
        return new RepaymentTransactionResponse(
                t.getRtxId(), t.getCntrId(), t.getRschId(),
                t.getRtxTypeCd(), t.getRtxStatusCd(),
                t.getTotalAmount(), t.getPrincipalAmount(), t.getInterestAmount(),
                t.getOverdueInterestAmount(), t.getFeeAmount(),
                t.getChannelCd(), t.getCurrencyCd(),
                t.getPaidAt(), t.getValueDate(), t.getBalanceAfter(),
                t.getIdempotencyKey()
        );
    }
}
