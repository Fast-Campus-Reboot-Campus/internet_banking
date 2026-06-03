package com.bank.loan.review.dto;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.security.PiiLevel;
import com.bank.loan.security.PiiMaskingUtil;

import java.math.BigDecimal;
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
        Long biasOverrideBy,
        String revAiTrackCd,
        BigDecimal revAiPd,
        String revAiRationale,
        // PII 필드 — PiiLevel 에 따라 차등 노출 (§3.3, §8)
        Long estimatedIncomeAmt,   // FULL/MASKED: 원금액, REDACTED: null
        String estimatedIncomeRange // REDACTED: 금액대 문자열, 그 외: null
) {
    /** 기존 팩토리 — PII 없이 심사 데이터만 반환. 내부 배치·OPS 호출 경로에서 사용. */
    public static LoanReviewResponse of(LoanReview r) {
        return new LoanReviewResponse(
                r.getRevId(), r.getApplId(),
                r.getRevTypeCd(), r.getRevStatusCd(), r.getRevDecisionCd(),
                r.getApprovedAmount(), r.getApprovedRateBps(), r.getApprovedPeriodMo(),
                r.getRejectReasonCd(), r.getRevRemark(),
                r.getReviewerId(), r.getReviewedAt(), r.getApprovedAt(),
                r.getApproverId(), r.getApprovedDecisionCd(), r.getOverrideReasonCd(),
                r.getBiasSeverityCd(), r.getBiasOverrideBy(),
                r.getRevAiTrackCd(), r.getRevAiPd(), r.getRevAiRationale(),
                null, null
        );
    }

    /**
     * PII 차등 노출 팩토리.
     * level 에 따라 estimatedIncomeAmt / estimatedIncomeRange 를 채운다.
     * 직원 대면 조회 경로(LoanReviewService.get)에서 사용.
     */
    public static LoanReviewResponse of(LoanReview r, LoanApplication app, PiiLevel level) {
        Long income = level == PiiLevel.REDACTED ? null : app.getEstimatedIncomeAmt();
        String range = level == PiiLevel.REDACTED
                ? PiiMaskingUtil.amountRange(app.getEstimatedIncomeAmt())
                : null;
        return new LoanReviewResponse(
                r.getRevId(), r.getApplId(),
                r.getRevTypeCd(), r.getRevStatusCd(), r.getRevDecisionCd(),
                r.getApprovedAmount(), r.getApprovedRateBps(), r.getApprovedPeriodMo(),
                r.getRejectReasonCd(), r.getRevRemark(),
                r.getReviewerId(), r.getReviewedAt(), r.getApprovedAt(),
                r.getApproverId(), r.getApprovedDecisionCd(), r.getOverrideReasonCd(),
                r.getBiasSeverityCd(), r.getBiasOverrideBy(),
                r.getRevAiTrackCd(), r.getRevAiPd(), r.getRevAiRationale(),
                income, range
        );
    }
}
