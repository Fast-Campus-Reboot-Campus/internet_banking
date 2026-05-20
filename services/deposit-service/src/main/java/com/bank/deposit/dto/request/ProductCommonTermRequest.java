package com.bank.deposit.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProductCommonTermRequest(
        @NotNull Long termsTemplateId,
        Boolean isRequired
) {}
