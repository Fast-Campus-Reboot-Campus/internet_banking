package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 본심사 체크 로그 수동 추가 요청.
 *
 * 자동 적재 5건(PRESCREEN_PASS/CB_DECISION/DSR_CHECK/LTV_CHECK/FINAL_DECISION) 외에
 * 심사관이 서류·신원·부수거래·기타 항목을 직접 기록할 때 사용.
 *
 * checkItemCd : DOCUMENT_CHECK | IDENTITY_CHECK | CROSS_TRANSACTION | ETC
 *               자동 적재 항목 코드를 전달하면 400 (덮어쓰기 방지).
 * checkResultCd : PASS | FAIL | REVIEW | N_A
 */
public record AddReviewCheckLogRequest(

        @NotBlank
        @Pattern(regexp = "DOCUMENT_CHECK|IDENTITY_CHECK|CROSS_TRANSACTION|ETC")
        String checkItemCd,

        @NotBlank
        @Pattern(regexp = "PASS|FAIL|REVIEW|N_A")
        String checkResultCd,

        @Size(max = 500)
        String checkRemark,

        Long checkerId
) {
}
