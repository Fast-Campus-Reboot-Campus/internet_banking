package com.bank.loan.repaymentaccount.dto;

import jakarta.validation.constraints.Size;

/**
 * 상환계좌 검증 요청. 외부 계좌검증 연계는 stub — 본 요청은 성공으로 처리한다.
 * verifyChannelCd / verifyRemark 는 감사 목적의 메모 용도.
 */
public record VerifyRepaymentAccountRequest(

        @Size(max = 50) String verifyChannelCd,

        @Size(max = 500) String verifyRemark
) {
}
