package com.bank.loan.application.dto;

import com.bank.loan.application.domain.LoanApplication;

import java.time.OffsetDateTime;

public record LoanApplicationResponse(
        Long applId,
        String applNo,
        Long customerId,
        Long prodId,
        String channelCd,
        Long requestedAmount,
        Integer requestedPeriodMo,
        String loanPurposeCd,
        String repaymentMethodCd,
        Long estimatedIncomeAmt,
        String employmentTypeCd,
        String applStatusCd,
        OffsetDateTime appliedAt
) {
    public static LoanApplicationResponse of(LoanApplication a) {
        return new LoanApplicationResponse(
                a.getApplId(), a.getApplNo(), a.getCustomerId(), a.getProdId(),
                a.getChannelCd(), a.getRequestedAmount(), a.getRequestedPeriodMo(),
                a.getLoanPurposeCd(), a.getRepaymentMethodCd(),
                a.getEstimatedIncomeAmt(), a.getEmploymentTypeCd(),
                a.getApplStatusCd(), a.getAppliedAt()
        );
    }
}
