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
        String code,

        /** 실명 — 신원확인(SIGNUP/IDENTITY_VERIFY) 목적 시 필수. */
        String name,

        /** 주민등록번호 13자리 — 신원확인 목적 시 필수. 평문은 저장하지 않고 CI·암호문으로 변환된다. */
        @Pattern(regexp = "\\d{13}", message = "주민등록번호는 13자리 숫자입니다.")
        String rrn
) {}
