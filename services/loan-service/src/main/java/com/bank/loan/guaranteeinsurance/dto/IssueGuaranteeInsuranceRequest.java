package com.bank.loan.guaranteeinsurance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 보증보험 발급 요청. 외부기관 stub — 즉시 ISSUED 처리.
 *
 *   ginsAgencyCd        보증기관 코드 (예: SGI, HUG, HF)
 *   guaranteeAmount     보증 금액 (1 이상)
 *   guaranteeRatioBps   보증 비율 bps (0~10000)
 *   premiumAmount       보험료 (0 이상)
 *   ginsStartDate       유효 시작일 YYYYMMDD (옵션 — 미지정 시 계약 시작일)
 *   ginsEndDate         유효 종료일 YYYYMMDD (옵션 — 미지정 시 계약 종료일)
 *   ginsDocUrl          증권 문서 URL (옵션)
 *   ginsDocHash         증권 문서 해시 (옵션)
 */
public record IssueGuaranteeInsuranceRequest(
        @NotBlank @Size(max = 50) String ginsAgencyCd,
        @NotNull @Min(1) Long guaranteeAmount,
        @NotNull @Min(0) @Max(10_000) Integer guaranteeRatioBps,
        @NotNull @Min(0) Long premiumAmount,
        @Pattern(regexp = "\\d{8}") String ginsStartDate,
        @Pattern(regexp = "\\d{8}") String ginsEndDate,
        @Size(max = 500) String ginsDocUrl,
        @Size(max = 128) String ginsDocHash
) {
}
