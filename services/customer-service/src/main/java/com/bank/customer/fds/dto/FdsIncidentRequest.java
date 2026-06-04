package com.bank.customer.fds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FdsIncidentRequest(
        @NotNull  Long   fdsDetectionId,
        @NotBlank String fdsIncidentTypeCode,
        Long             handlerEmployeeId
) {}
