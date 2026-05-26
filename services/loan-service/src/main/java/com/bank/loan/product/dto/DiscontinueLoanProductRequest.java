package com.bank.loan.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 상품 단종 요청.
 *   saleEndDate : 판매 종료일 (YYYYMMDD)
 *   reasonCd    : 단종 사유 코드 (status_history.change_reason_cd 기록용)
 *   reasonRemark: 자유 기술 비고
 */
public record DiscontinueLoanProductRequest(
        @NotBlank @Pattern(regexp = "\\d{8}") String saleEndDate,
        @NotBlank @Size(max = 50)             String reasonCd,
        @Size(max = 500)                       String reasonRemark
) {
}
