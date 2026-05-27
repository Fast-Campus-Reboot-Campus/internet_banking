package com.bank.loan.reversal.dto;

import jakarta.validation.constraints.Size;

/**
 * 상환 거래 역분개 요청.
 *
 *   reversalReasonCd   사유 코드 (예: MISTAKE, DUPLICATE, EXTERNAL_CHARGEBACK). 옵션.
 *   reversalRemark     비고 (옵션, 500자).
 */
public record ReverseRepaymentRequest(
        @Size(max = 50) String reversalReasonCd,
        @Size(max = 500) String reversalRemark
) {
}
