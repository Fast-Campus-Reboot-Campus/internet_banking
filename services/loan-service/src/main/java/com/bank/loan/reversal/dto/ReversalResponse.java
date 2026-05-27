package com.bank.loan.reversal.dto;

import com.bank.loan.repayment.domain.RepaymentTransaction;

import java.time.OffsetDateTime;

/**
 * 역분개 결과.
 *
 *   reversalRtxId           신규 생성된 역분개 row PK
 *   targetRtxId             정정 대상 원본 거래 PK
 *   cntrId                  계약 PK
 *   restoredRschId          PAID → DUE 로 되돌린 단일 회차 PK (SCHEDULED 정정 시. EARLY 정정에선 null)
 *   amount                  역분개 총액 (원본과 동일 양수 — 회계 반대분개는 별도 책임)
 *   reversedAt              처리 시각
 *
 * EARLY 정정 시 추가 정보 (SCHEDULED 정정에선 null/0):
 *   supersededVersionCd     V 되돌리기로 SUPERSEDED 된 V_new 코드 (예: V2)
 *   supersededCount         V_new 에서 SUPERSEDED 된 회차 수
 *   restoredVersionCd       V_new 이전 버전인 V_prev 코드 (예: V1)
 *   restoredCount           V_prev 에서 DUE 로 부활된 회차 수
 */
public record ReversalResponse(
        Long reversalRtxId,
        Long targetRtxId,
        Long cntrId,
        Long restoredRschId,
        Long amount,
        OffsetDateTime reversedAt,
        String supersededVersionCd,
        Integer supersededCount,
        String restoredVersionCd,
        Integer restoredCount
) {
    public static ReversalResponse of(RepaymentTransaction reversalTx, Long restoredRschId) {
        return new ReversalResponse(
                reversalTx.getRtxId(),
                reversalTx.getReversalTargetRtxId(),
                reversalTx.getCntrId(),
                restoredRschId,
                reversalTx.getTotalAmount(),
                reversalTx.getPaidAt(),
                null, null, null, null
        );
    }

    public static ReversalResponse ofEarly(RepaymentTransaction reversalTx,
                                           String supersededVersionCd, int supersededCount,
                                           String restoredVersionCd, int restoredCount) {
        return new ReversalResponse(
                reversalTx.getRtxId(),
                reversalTx.getReversalTargetRtxId(),
                reversalTx.getCntrId(),
                null,
                reversalTx.getTotalAmount(),
                reversalTx.getPaidAt(),
                supersededVersionCd, supersededCount,
                restoredVersionCd, restoredCount
        );
    }
}
