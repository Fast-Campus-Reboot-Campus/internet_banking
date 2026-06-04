package com.bank.customer.party.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddPartyRelationRequest(
        @NotNull  Long   toPartyId,
        @NotBlank String relationTypeCode,
        String            relationDetailCode,
        Integer           equityRatioBps,
        String            representationScope,
        String            proofUrl,
        @NotBlank String  relationStartDate   // YYYYMMDD
) {}
