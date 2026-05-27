package com.bank.loan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproverApproveRequest(
        @NotNull Long approverId,
        @NotBlank String approverDecisionCd,
        String overrideReasonCd,
        String overrideRemark,
        Long overrideAmount,
        Integer overrideRateBps,
        Integer overridePeriodMo,
        String overrideRejectReasonCd
) {}
