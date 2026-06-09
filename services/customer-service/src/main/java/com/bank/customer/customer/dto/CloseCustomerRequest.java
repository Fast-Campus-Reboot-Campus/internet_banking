package com.bank.customer.customer.dto;

import jakarta.validation.constraints.NotBlank;

/** 회원 해지(탈퇴) 요청. closeReasonCode는 customer.close_reason_code로 기록된다. */
public record CloseCustomerRequest(
        @NotBlank String closeReasonCode,
        String reasonDetail
) {}
