package com.bank.loan.review.dto;

import com.bank.loan.review.domain.LoanReview;

import java.time.OffsetDateTime;

public record LoanReviewResponse(
        Long revId,
        Long applId,
        String revTypeCd,
        String revStatusCd,
        String revDecisionCd,
        Long approvedAmount,
        Integer approvedRateBps,
        Integer approvedPeriodMo,
        String rejectReasonCd,
        String revRemark,
        Long reviewerId,
        OffsetDateTime reviewedAt,
        OffsetDateTime approvedAt,
        Long approverId,
        String approvedDecisionCd,
        String overrideReasonCd,
        String biasSeverityCd,
        Long biasOverrideBy
) {
    public static LoanReviewResponse of(LoanReview r) {
        return new LoanReviewResponse(
                r.getRevId(), r.getApplId(),
                r.getRevTypeCd(), r.getRevStatusCd(), r.getRevDecisionCd(),
                r.getApprovedAmount(), r.getApprovedRateBps(), r.getApprovedPeriodMo(),
                r.getRejectReasonCd(), r.getRevRemark(),
                r.getReviewerId(), r.getReviewedAt(), r.getApprovedAt(),
                r.getApproverId(), r.getApprovedDecisionCd(), r.getOverrideReasonCd(),
                r.getBiasSeverityCd(), r.getBiasOverrideBy()
        );
    }
}
