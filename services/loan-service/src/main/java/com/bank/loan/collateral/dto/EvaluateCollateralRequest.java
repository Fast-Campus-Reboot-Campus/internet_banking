package com.bank.loan.collateral.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 담보 감정평가 요청.
 *   appraisedValue : 감정평가금액 (필수)
 *   appliedValue   : 적용담보가액 (미지정 시 appraisedValue 와 동일 적용)
 */
public record EvaluateCollateralRequest(

        @NotBlank @Size(max = 50) String evalMethodCd,
        @Size(max = 50)           String evalAgencyCd,

        @NotNull @Min(0) Long appraisedValue,
        @Min(0)          Long appliedValue,

        @Size(max = 500) String evalReportUrl,
        @Size(max = 128) String evalReportHash,

        @Pattern(regexp = "\\d{8}") String appliedStartDate,
        @Pattern(regexp = "\\d{8}") String appliedEndDate
) {
}
