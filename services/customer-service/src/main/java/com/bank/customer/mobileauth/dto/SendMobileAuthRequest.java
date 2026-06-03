package com.bank.customer.mobileauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendMobileAuthRequest(
        @NotBlank
        @Pattern(regexp = "010\\d{8}", message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber,
        @NotBlank String telecomCarrierCode,
        @NotBlank String purposeCode,
        /** SMS | PASS */
        @NotBlank String methodTypeCode
) {}
