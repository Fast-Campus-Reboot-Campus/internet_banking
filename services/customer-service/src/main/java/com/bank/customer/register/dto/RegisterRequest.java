package com.bank.customer.register.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(min = 4, max = 20)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "영문, 숫자, 밑줄(_)만 허용합니다.")
        String loginId,

        @NotBlank
        @Size(min = 8, max = 30)
        String password,

        /**
         * 휴대폰+주민번호 본인확인(/mobile-auth/verify) 결과 id.
         * 이름·생년월일·성별·CI·주민번호는 클라이언트가 아니라 이 검증 이력이 권위 소스다.
         */
        @NotNull
        Long verificationId,

        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 10~11자리 숫자입니다.")
        String phone,

        @Email
        @Size(max = 255)
        String email
) {}
