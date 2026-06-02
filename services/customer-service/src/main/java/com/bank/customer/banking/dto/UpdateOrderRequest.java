package com.bank.customer.banking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateOrderRequest(
        @NotNull
        List<Long> orderedIds
) {}
