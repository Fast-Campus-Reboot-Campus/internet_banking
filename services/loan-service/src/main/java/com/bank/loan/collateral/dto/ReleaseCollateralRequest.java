package com.bank.loan.collateral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 담보 해제 요청.
 *   releaseReasonCd : 해제 사유 코드 (status_history.change_reason_cd 기록용)
 *   releaseDate     : 해제 일자 (YYYYMMDD) — 비고에 함께 기록
 *   releaseRemark   : 자유 기술 비고
 */
public record ReleaseCollateralRequest(
        @NotBlank @Size(max = 50)  String releaseReasonCd,
        @Pattern(regexp = "\\d{8}") String releaseDate,
        @Size(max = 400)            String releaseRemark
) {
}
