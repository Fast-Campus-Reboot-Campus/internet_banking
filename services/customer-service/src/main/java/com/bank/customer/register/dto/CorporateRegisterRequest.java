package com.bank.customer.register.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CorporateRegisterRequest(
        /** 법인명 */
        @NotBlank String corpName,
        @NotBlank String corpEnglishName,

        /** 법인등록번호 14자리 */
        @NotBlank @Pattern(regexp = "\\d{6}-\\d{7}") String corpRegNo,

        /** 사업자등록번호 */
        @NotBlank @Pattern(regexp = "\\d{3}-\\d{2}-\\d{5}") String bizRegNo,

        @NotBlank String tradeName,
        @NotBlank String openingDate,    // YYYYMMDD
        @NotBlank String ntsIndustryCode,
        @NotBlank String ksicCode,
        @NotBlank String bizItemCode,
        @NotBlank String taxTypeCode,

        /** 대표 계정 정보 */
        @NotBlank String loginId,
        @NotBlank String password,
        @NotBlank String email,
        @NotBlank String phone
) {}
