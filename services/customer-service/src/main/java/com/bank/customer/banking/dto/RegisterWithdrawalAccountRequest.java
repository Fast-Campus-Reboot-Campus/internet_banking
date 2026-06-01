package com.bank.customer.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterWithdrawalAccountRequest(

        @NotBlank
        @Pattern(regexp = "\\d{10,14}", message = "계좌번호는 10~14자리 숫자여야 합니다.")
        String accountNumber,

        @NotBlank
        @Size(max = 10)
        String bankCode,

        @NotBlank
        @Size(max = 50)
        String bankName,

        @Size(max = 100)
        String accountHolderName,

        @Size(max = 100)
        String accountAlias
) {}
