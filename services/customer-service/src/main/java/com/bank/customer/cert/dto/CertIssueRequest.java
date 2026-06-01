package com.bank.customer.cert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CertIssueRequest(

        @NotBlank
        String loginId,

        @NotBlank
        String password,

        @NotBlank
        @Pattern(regexp = "CERT_COMMON|CERT_FIN|CERT_AXFUL", message = "certType은 CERT_COMMON, CERT_FIN, CERT_AXFUL 이어야 합니다.")
        String certType,

        @NotBlank
        @Size(min = 8, max = 30, message = "인증서 암호는 8~30자로 입력해 주세요.")
        String certPin
) {}
