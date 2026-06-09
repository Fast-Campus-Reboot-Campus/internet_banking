package com.bank.loan.review.service;

import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.dsr.domain.DsrCalculation;
import com.bank.loan.ltv.domain.LtvCalculation;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.review.domain.ReviewCheckLog;
import com.bank.loan.review.dto.ReviseReviewRequest;
import com.bank.loan.review.dto.RunReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * REVIEW_CHECK_LOG 자동 적재의 "스크립트" 계층 — 결정 경로별(manual/auto/revisit) 묶음을 기록.
 * 단건 row 적재는 ReviewCheckLogger 가 처리.
 */
@Component
@RequiredArgsConstructor
public class LoanReviewCheckLogWriter {

    private final ReviewCheckLogger reviewCheckLogger;

    public void logManual(Long revId, CreditEvaluation ceval, DsrCalculation dsr,
                          LoanProduct product, boolean approved, RunReviewRequest req,
                          Long checkerId) {

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_PRESCREEN_PASS, ReviewCheckLog.RESULT_PASS,
                "application=PRESCREENED", checkerId);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_CB_DECISION,
                CreditEvaluation.DECISION_APPROVE.equals(ceval.getCevalDecisionCd())
                        ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_REVIEW,
                "decision=" + ceval.getCevalDecisionCd() + ", engine=" + ceval.getCevalEngine(),
                checkerId);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_DSR_CHECK, ReviewCheckLog.RESULT_PASS,
                "ratioBps=" + dsr.getDsrRatioBps() + ", limit=" + dsr.getDsrLimitBps(),
                checkerId);

        if (product != null && product.isCollateralRequired()) {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK, ReviewCheckLog.RESULT_PASS,
                    "all active collaterals LTV PASS", checkerId);
        } else {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK, ReviewCheckLog.RESULT_N_A,
                    "collateral not required", checkerId);
        }

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "decision=" + req.revDecisionCd()
                        + (approved ? "" : ", rejectReasonCd=" + req.rejectReasonCd()),
                checkerId);
    }

    public void logAuto(Long revId, CreditEvaluation ceval, DsrCalculation dsr,
                        LtvCalculation ltv, boolean collateralRequired,
                        boolean approved, String autoRejectReasonCd) {
        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_PRESCREEN_PASS, ReviewCheckLog.RESULT_PASS,
                "application=PRESCREENED, auto", null);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_CB_DECISION,
                CreditEvaluation.DECISION_APPROVE.equals(ceval.getCevalDecisionCd())
                        ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "decision=" + ceval.getCevalDecisionCd() + ", engine=" + ceval.getCevalEngine(),
                null);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_DSR_CHECK,
                DsrCalculation.STATUS_PASS.equals(dsr.getDsrStatusCd())
                        ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "ratioBps=" + dsr.getDsrRatioBps() + ", limit=" + dsr.getDsrLimitBps(),
                null);

        if (collateralRequired) {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK,
                    LtvCalculation.STATUS_PASS.equals(ltv.getLtvStatusCd())
                            ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                    "ltvStatus=" + ltv.getLtvStatusCd() + ", colId=" + ltv.getColId(),
                    null);
        } else {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK, ReviewCheckLog.RESULT_N_A,
                    "collateral not required", null);
        }

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "auto, decision=" + (approved ? "APPROVED" : "REJECTED")
                        + (approved ? "" : ", rejectReasonCd=" + autoRejectReasonCd),
                null);
    }

    /** 자동 권고 확정 시 FINAL_DECISION 한 줄 append — 권고 단계 로그는 그대로 보존. */
    public void logConfirm(Long revId, boolean approved, Long reviewerId, String confirmRemark) {
        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "confirmed by reviewerId=" + reviewerId
                        + (confirmRemark != null ? ", remark=" + confirmRemark : ""),
                reviewerId);
    }

    /** 정정 시 FINAL_DECISION 한 줄만 append — 기존 5건은 그대로 누적. actorId 는 인증된 정정 행위자. */
    public void logRevisit(Long revId, boolean approved, ReviseReviewRequest req, Long actorId) {
        String revisitTag = "revisit(" + req.revisitReasonCd() + ")";
        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                revisitTag + ", decision=" + req.revDecisionCd()
                        + (approved ? "" : ", rejectReasonCd=" + req.rejectReasonCd()),
                actorId);
    }
}
