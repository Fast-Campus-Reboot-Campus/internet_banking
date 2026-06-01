package com.bank.customer.cert.dto;

import jakarta.validation.constraints.NotBlank;

public record CertLoginRequest(
        @NotBlank String certSerialNumber,
        @NotBlank String pin,
        @NotBlank String certType   // CERT_COMMON | CERT_FIN | CERT_AXFUL
) {}
