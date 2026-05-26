package com.bank.customer.register.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

        @NotBlank
        @Size(max = 100)
        String name,

        /** YYYYMMDD */
        @NotBlank
        @Pattern(regexp = "^\\d{8}$", message = "생년월일은 YYYYMMDD 8자리 숫자입니다.")
        String birthDate,

        /** M / F / U */
        @NotBlank
        @Pattern(regexp = "^[MFU]$", message = "성별코드는 M, F, U 중 하나입니다.")
        String genderCode,

        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 10~11자리 숫자입니다.")
        String phone,

        @Email
        @Size(max = 255)
        String email
) {}
