package com.bank.customer.cert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertPinChangeRequest(

        @NotBlank
        String certSerialNumber,

        @NotBlank
        String currentPin,

        @NotBlank
        @Size(min = 6, max = 30, message = "새 인증서 암호는 6~30자로 입력해 주세요.")
        String newPin
) {}
