package com.bank.customer.cert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CertIssueRequest(

        @NotBlank
        String loginId,

        @NotBlank
        String password,

        @NotBlank
        @Pattern(regexp = "CERT_COMMON|CERT_FIN|CERT_AXFUL", message = "certType은 CERT_COMMON, CERT_FIN, CERT_AXFUL 이어야 합니다.")
        String certType
) {}
