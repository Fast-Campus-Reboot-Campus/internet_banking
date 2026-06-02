package com.bank.loan.commonsync.dto;

import java.time.OffsetDateTime;

/**
 * TRANSACTION 타입 outbox payload.
 * LoanExecution 또는 RepaymentTransaction 스냅샷 → CommonTransaction 생성에 필요한 필드.
 *
 * sourceTable: "loan_execution" | "repayment_transaction"
 *   — 브리지 백필 시 어느 loan_db 테이블을 갱신할지 구분하는 데 사용.
 * cntrId: loan_db FK — 디스패처가 LoanContract.contractId(common PK)를 역참조하는 데 사용.
 */
public record TransactionSyncPayload(
        String sourceTable,
        String transactionNo,
        Long cntrId,
        String transactionTypeCd,
        String debitCreditType,
        Long transactionAmount,
        Long balanceBefore,
        Long balanceAfter,
        Long feeAmount,
        String channelCd,
        String counterpartyBankCd,
        String counterpartyAccountNo,
        String counterpartyName,
        String transactionMemo,
        String transactionStatus,
        OffsetDateTime transactedAt,
        String currencyCd,
        Long availableBalance,
        String transactionSummary,
        String transferTypeCd,
        String failureTypeCd,
        String failureReasonCd,
        OffsetDateTime failedAt,
        String approvalNo,
        OffsetDateTime ledgerPostedAt
) {
    public static final String SOURCE_EXECUTION   = "loan_execution";
    public static final String SOURCE_REPAYMENT   = "repayment_transaction";
}
