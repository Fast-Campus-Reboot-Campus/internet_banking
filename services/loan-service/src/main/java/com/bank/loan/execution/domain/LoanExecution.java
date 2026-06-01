package com.bank.loan.execution.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 대출 실행 (자금 인출 / Drawdown). ERD STAGE 6 LOAN_EXECUTION 매핑.
 *
 * 한 약정에 여러 번 실행 가능 (트랜치). idempotency_key 로 중복 호출 방어.
 *
 * transaction_id 는 공통 거래원장 FK — 결제 도메인 미구현 단계에서는 nullable.
 */
@Getter
@Entity
@Table(name = "loan_execution")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanExecution extends BaseEntity {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_DONE      = "DONE";
    public static final String STATUS_FAILED    = "FAILED";
    public static final String STATUS_CANCELED  = "CANCELED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exec_id")
    private Long execId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "executed_amount", nullable = false)
    private Long executedAmount;

    @Column(name = "currency_cd", nullable = false, length = 10)
    private String currencyCd;

    @Column(name = "exec_status_cd", nullable = false, length = 50)
    private String execStatusCd;

    @Column(name = "disbursement_bank_cd", length = 10)
    private String disbursementBankCd;

    @Column(name = "disbursement_account_enc")
    private byte[] disbursementAccountEnc;

    @Column(name = "disbursement_account_masked", length = 50)
    private String disbursementAccountMasked;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "value_date", length = 8)
    private String valueDate;

    @Column(name = "fee_amount", nullable = false)
    private Long feeAmount;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "journal_entry_no", length = 50)
    private String journalEntryNo;

    @Column(name = "pi_id", length = 100)
    private String piId;

    public void markDone(String piId, String journalEntryNo) {
        this.execStatusCd = STATUS_DONE;
        this.piId = piId;
        this.journalEntryNo = journalEntryNo;
        this.executedAt = OffsetDateTime.now();
    }

    public void markFailed(String piId) {
        this.execStatusCd = STATUS_FAILED;
        this.piId = piId;
    }
}
