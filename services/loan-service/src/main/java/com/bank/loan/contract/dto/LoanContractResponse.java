package com.bank.loan.contract.dto;

import com.bank.loan.contract.domain.LoanContract;

import java.time.OffsetDateTime;

public record LoanContractResponse(
        Long cntrId,
        String cntrNo,
        Long applId,
        Long revId,
        Long customerId,
        Long prodId,
        Long contractedAmount,
        String currencyCd,
        Integer contractedPeriodMo,
        Integer totalRateBps,
        Integer baseRateBps,
        Integer spreadBps,
        Integer preferentialRateBps,
        String rateTypeCd,
        String repaymentMethodCd,
        String cntrStatusCd,
        String cntrStartDate,
        String cntrEndDate,
        String cntrDocUrl,
        String cntrDocHash,
        OffsetDateTime signedAt
) {
    public static LoanContractResponse of(LoanContract c) {
        return new LoanContractResponse(
                c.getCntrId(), c.getCntrNo(),
                c.getApplId(), c.getRevId(),
                c.getCustomerId(), c.getProdId(),
                c.getContractedAmount(), c.getCurrencyCd(),
                c.getContractedPeriodMo(),
                c.getTotalRateBps(), c.getBaseRateBps(), c.getSpreadBps(), c.getPreferentialRateBps(),
                c.getRateTypeCd(), c.getRepaymentMethodCd(),
                c.getCntrStatusCd(),
                c.getCntrStartDate(), c.getCntrEndDate(),
                c.getCntrDocUrl(), c.getCntrDocHash(),
                c.getSignedAt()
        );
    }
}
