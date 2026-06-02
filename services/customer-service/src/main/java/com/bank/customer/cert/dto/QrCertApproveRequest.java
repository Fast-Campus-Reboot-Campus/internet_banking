package com.bank.customer.cert.dto;

import jakarta.validation.constraints.NotBlank;

public record QrCertApproveRequest(
        @NotBlank String tokenHash,
        @NotBlank String loginId,
        @NotBlank String password
) {}
