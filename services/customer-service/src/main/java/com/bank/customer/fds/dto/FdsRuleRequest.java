package com.bank.customer.fds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FdsRuleRequest(
        @NotBlank String fdsRuleCode,
        @NotBlank String fdsRuleName,
        @NotBlank String fdsRuleCategoryCode,
        @NotBlank String fdsRuleTargetEventCode,
        @NotBlank String fdsRuleConditionJson,
        @NotNull  Integer fdsRuleRiskWeight,
        @NotBlank String fdsRuleActionTypeCode,
        @NotBlank String fdsRuleEffectiveDate,
        String fdsRuleExpiryDate
) {}
