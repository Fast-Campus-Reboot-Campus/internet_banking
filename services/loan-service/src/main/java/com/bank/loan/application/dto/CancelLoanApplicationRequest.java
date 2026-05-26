package com.bank.loan.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 신청 취소 요청.
 *   cancelReasonCd : 취소 사유 코드 (status_history.change_reason_cd 기록용)
 *   cancelRemark   : 자유 기술 비고
 */
public record CancelLoanApplicationRequest(
        @NotBlank @Size(max = 50) String cancelReasonCd,
        @Size(max = 500)          String cancelRemark
) {
}
