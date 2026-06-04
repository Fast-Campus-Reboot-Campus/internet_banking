package com.bank.customer.fds.dto;

import com.bank.customer.fds.domain.FdsRule;

public record FdsRuleResponse(
        Long   fdsRuleId,
        String fdsRuleCode,
        String fdsRuleName,
        String fdsRuleCategoryCode,
        String fdsRuleTargetEventCode,
        String fdsRuleConditionJson,
        int    fdsRuleRiskWeight,
        String fdsRuleActionTypeCode,
        boolean active,
        String fdsRuleEffectiveDate,
        String fdsRuleExpiryDate
) {
    public static FdsRuleResponse from(FdsRule r) {
        return new FdsRuleResponse(
                r.getFdsRuleId(), r.getFdsRuleCode(), r.getFdsRuleName(),
                r.getFdsRuleCategoryCode(), r.getFdsRuleTargetEventCode(),
                r.getFdsRuleConditionJson(), r.getFdsRuleRiskWeight(),
                r.getFdsRuleActionTypeCode(), r.isActive(),
                r.getFdsRuleEffectiveDate(), r.getFdsRuleExpiryDate());
    }
}
