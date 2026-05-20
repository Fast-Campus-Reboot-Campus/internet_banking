package com.bank.payment.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌원장 (ledger)
 *
 * 거래분개 (복식부기). 차변/대변 분개 단위.
 * - V3__create_ledger.sql 정합
 * - 컬럼명세서 v12.2 기반
 * - payment_instruction FK (NULL 허용, 이자/수기분개 시 NULL)
 * - original_ledger_id self FK (역분개 시 원분개 참조)
 * - 비즈니스 메서드는 Stage 5에서 추가
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Ledger {

    // 분개번호
    private String ledgerId;

    // 결제지시번호
    private String paymentInstructionId;

    // 계좌번호
    private String accountId;

    // 원분개참조
    private String originalLedgerId;

    // 회계번호
    private String journalNo;

    // 계좌번호_스냅샷
    private String accountNoSnap;

    // 예금주명_스냅샷
    private String holderNameSnap;

    // 차변대변구분
    private String debitCredit;

    // 분개종류
    private String journalType;

    // 금액
    private BigDecimal amount;

    // 통화
    private String currency;

    // 분개직전잔액
    private BigDecimal balanceBefore;

    // 분개직후잔액
    private BigDecimal balanceAfter;

    // 상대계좌번호_스냅샷
    private String counterpartyAccountNoSnap;

    // 상대은행코드_스냅샷
    private String counterpartyBankCodeSnap;

    // 상대예금주명_스냅샷
    private String counterpartyHolderNameSnap;

    // 거래일자
    private String transactionDate;

    // 기장일자
    private String postingDate;

    // 자금가용일
    private String valueDate;

    // 기장시각
    private LocalDateTime postedAt;

    // 시스템적요
    private String systemDescription;

    // 통장에찍히는메모_스냅샷
    private String passbookMemoSnap;

    // 역분개여부
    private Boolean isReversal;

    // 역분개사유
    private String reversalReason;

    // 기장상태
    private String postingStatus;

    // 최초등록일시
    private LocalDateTime firstRegisteredAt;

    // 최초등록자식별번호
    private String firstRegistrantId;

    // 최종수정일시
    private LocalDateTime lastModifiedAt;

    // 최종수정자식별번호
    private String lastModifierId;
}
