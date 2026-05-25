package com.bank.loan.closure.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 약정 종결 요청.
 *
 * closureDate 미지정 시 서버가 today 사용. final_principal/interest 는 서버가 자동 산출 —
 * 사용자 입력은 fee/prepayment_fee 와 종결 사유·문서만.
 */
public record CloseLoanRequest(

        @NotBlank @Size(max = 50) String closureTypeCd,

        @Size(max = 50) String closureReasonCd,

        @Pattern(regexp = "\\d{8}") String closureDate,

        @Min(0) Long finalFeeAmt,
        @Min(0) Long prepaymentFeeAmt,

        @Size(max = 500) String closureDocUrl,
        @Size(max = 128) String closureDocHash,

        @Size(max = 200) String subrogationPartyRef,
        @Size(max = 50)  String writeOffReasonCd
) {
}
