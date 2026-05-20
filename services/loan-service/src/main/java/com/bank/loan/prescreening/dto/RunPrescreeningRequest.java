package com.bank.loan.prescreening.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 가심사 실행 요청. 외부 엔진 연계 전 단계 — 결과를 클라이언트가 입력으로 결정.
 *
 * estimatedLimitAmt / estimatedRateBps 미지정 시 서버가 자동 추정:
 *   PASS: estimatedLimitAmt = requestedAmount, estimatedRateBps = product.baseRateBps
 *   REJECT: 두 값 모두 무시 — null 로 저장
 */
public record RunPrescreeningRequest(

        @NotBlank @Pattern(regexp = "PASS|REJECT") String prescResultCd,

        @Min(0) Long estimatedLimitAmt,
        @Min(0) Integer estimatedRateBps,

        @Size(max = 10) String estimatedGrade,
        @Min(0) Integer estimatedScore,

        @Size(max = 50) String rejectReasonCd,
        @Size(max = 500) String prescRemark,

        @Size(max = 50) String prescEngineVersion
) {
}
