package com.bank.customer.pin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PinLoginRequest(
        @NotBlank String loginId,
        @NotNull  Long   deviceId,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "PIN은 6자리 숫자여야 합니다.")
        String pin
) {}
