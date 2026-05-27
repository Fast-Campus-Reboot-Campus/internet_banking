package com.bank.loan.idv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyIdentityRequest(

        @NotBlank @Size(max = 50) String idvMethodCd,
        @NotBlank @Size(max = 50) String idvTargetCd,

        @NotBlank @Pattern(regexp = "\\d{10,11}") String mobileNo
) {
}
