package com.bank.customer.mobileauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyMobileAuthRequest(
        @NotBlank
        @Pattern(regexp = "010\\d{8}")
        String phoneNumber,
        @NotBlank String purposeCode,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다.")
        String code
) {}
