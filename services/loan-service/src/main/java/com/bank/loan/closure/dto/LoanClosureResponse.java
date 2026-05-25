package com.bank.loan.closure.dto;

import com.bank.loan.closure.domain.LoanClosure;

import java.time.OffsetDateTime;

public record LoanClosureResponse(
        Long closId,
        Long cntrId,
        String closTypeCd,
        String closReasonCd,
        String closStatusCd,
        Long finalPrincipalAmt,
        Long finalInterestAmt,
        Long finalFeeAmt,
        Long prepaymentFeeAmt,
        Long totalSettledAmt,
        String closDate,
        OffsetDateTime closedAt,
        String closDocUrl,
        String closDocHash,
        Long writeOffAmount,
        Long subrogationAmount,
        String subrogationPartyRef,
        String writeOffReasonCd
) {
    public static LoanClosureResponse of(LoanClosure c) {
        return new LoanClosureResponse(
                c.getClosId(), c.getCntrId(),
                c.getClosTypeCd(), c.getClosReasonCd(), c.getClosStatusCd(),
                c.getFinalPrincipalAmt(), c.getFinalInterestAmt(),
                c.getFinalFeeAmt(), c.getPrepaymentFeeAmt(), c.getTotalSettledAmt(),
                c.getClosDate(), c.getClosedAt(),
                c.getClosDocUrl(), c.getClosDocHash(),
                c.getWriteOffAmount(), c.getSubrogationAmount(),
                c.getSubrogationPartyRef(), c.getWriteOffReasonCd()
        );
    }
}
