package com.bank.loan.execution.dto;

import com.bank.loan.execution.domain.LoanExecution;

import java.time.OffsetDateTime;

public record LoanExecutionResponse(
        Long execId,
        Long cntrId,
        Long executedAmount,
        Long cumulativeExecutedAmount,
        String currencyCd,
        String execStatusCd,
        String disbursementBankCd,
        String disbursementAccountMasked,
        OffsetDateTime executedAt,
        String valueDate,
        Long feeAmount,
        String journalEntryNo
) {
    public static LoanExecutionResponse of(LoanExecution e, long cumulative) {
        return new LoanExecutionResponse(
                e.getExecId(), e.getCntrId(),
                e.getExecutedAmount(), cumulative,
                e.getCurrencyCd(), e.getExecStatusCd(),
                e.getDisbursementBankCd(), e.getDisbursementAccountMasked(),
                e.getExecutedAt(), e.getValueDate(),
                e.getFeeAmount(), e.getJournalEntryNo()
        );
    }
}
