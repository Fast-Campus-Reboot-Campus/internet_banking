package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproverApproveRequest(
        @NotBlank String approverDecisionCd,
        String overrideReasonCd,
        String overrideRemark,
        Long overrideAmount,
        Integer overrideRateBps,
        Integer overridePeriodMo,
        String overrideRejectReasonCd
) {}
