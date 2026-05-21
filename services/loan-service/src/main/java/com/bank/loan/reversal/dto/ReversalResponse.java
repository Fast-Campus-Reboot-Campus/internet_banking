package com.bank.loan.reversal.dto;

import com.bank.loan.repayment.domain.RepaymentTransaction;

import java.time.OffsetDateTime;

/**
 * 역분개 결과.
 *
 *   reversalRtxId      신규 생성된 역분개 row PK
 *   targetRtxId        정정 대상 원본 거래 PK
 *   cntrId             계약 PK
 *   restoredRschId     PAID → DUE 로 되돌린 회차 PK (있는 경우)
 *   amount             역분개 총액 (원본과 동일 양수 — 회계 반대분개는 별도 책임)
 *   reversedAt         처리 시각
 */
public record ReversalResponse(
        Long reversalRtxId,
        Long targetRtxId,
        Long cntrId,
        Long restoredRschId,
        Long amount,
        OffsetDateTime reversedAt
) {
    public static ReversalResponse of(RepaymentTransaction reversalTx, Long restoredRschId) {
        return new ReversalResponse(
                reversalTx.getRtxId(),
                reversalTx.getReversalTargetRtxId(),
                reversalTx.getCntrId(),
                restoredRschId,
                reversalTx.getTotalAmount(),
                reversalTx.getPaidAt()
        );
    }
}
