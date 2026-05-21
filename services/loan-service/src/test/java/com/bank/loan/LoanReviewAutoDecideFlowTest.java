package com.bank.loan;

import com.bank.loan.support.AbstractLoanIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 본심사 자동 결정 통합 테스트.
 *
 * 시나리오:
 *   10) 자동 APPROVED — CB.APPROVE + DSR.PASS (신용대출, 한도 자동 산정)
 *   11) 자동 REJECTED — CB.REJECT (reason=CB_REJECT)
 *   12) 자동 REJECTED — DSR.FAIL (reason=DSR_OVER)
 *   13) 자동 REJECTED — 담보 필수 + LTV.FAIL (reason=LTV_FAIL)
 *   14) 자동 결정 불가 — CB.REVIEW → 422 LOAN_048
 *   15) 자동 결정 불가 — 본심사 이미 수행 → 409 LOAN_039
 *   16) 자동 결정 불가 — 미존재 applId → 404 LOAN_012
 *   17) 자동 결정 불가 — CB 미수행(데이터 부족) → 422 LOAN_038
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanReviewAutoDecideFlowTest extends AbstractLoanIntegrationTest {

    private static final long AMOUNT  = 30_000_000L;
    private static final int  MONTHS  = 36;
    private static final int  BASE_BPS = 500;

    private Long creditProdId;
    private Long mortgageProdId;

    private Long approveApplId;       // CB.APPROVE + DSR.PASS → 자동 APPROVED
    private Long cbRejectApplId;      // CB.REJECT → 자동 REJECTED
    private Long dsrFailApplId;       // DSR.FAIL → 자동 REJECTED
    private Long ltvFailApplId;       // 담보 LTV.FAIL → 자동 REJECTED
    private Long cbReviewApplId;      // CB.REVIEW → 422 LOAN_048
    private Long alreadyReviewedAppl; // 이미 본심사 → 409
    private Long noCevalApplId;       // CB 미수행 → 422 LOAN_038

    @org.junit.jupiter.api.BeforeAll
    void setup() throws Exception {
        creditProdId = createCreditProduct();
        activateProduct(creditProdId);
        mortgageProdId = createMortgageProduct();
        activateProduct(mortgageProdId);

        approveApplId       = createApplication(creditProdId, 13001);
        cbRejectApplId      = createApplication(creditProdId, 13002);
        dsrFailApplId       = createApplication(creditProdId, 13003);
        cbReviewApplId      = createApplication(creditProdId, 13004);
        alreadyReviewedAppl = createApplication(creditProdId, 13005);
        noCevalApplId       = createApplication(creditProdId, 13006);
        ltvFailApplId       = createApplication(mortgageProdId, 13007);

        // CB.APPROVE + DSR.PASS (정상)
        prepEligible(approveApplId, "APPROVE", 50_000_000L, /*dsrFail*/ false);

        // CB.REJECT
        prepEligible(cbRejectApplId, "REJECT", 50_000_000L, /*dsrFail*/ false);

        // DSR.FAIL
        prepEligible(dsrFailApplId, "APPROVE", 50_000_000L, /*dsrFail*/ true);

        // CB.REVIEW
        prepEligible(cbReviewApplId, "REVIEW", 50_000_000L, /*dsrFail*/ false);

        // 이미 본심사 — 정상 신청 + 본심사 1회 수동 실행
        prepEligible(alreadyReviewedAppl, "APPROVE", 50_000_000L, /*dsrFail*/ false);
        runReviewApprovedManually(alreadyReviewedAppl);

        // CB 미수행 — 가심사만
        runPrescreening(noCevalApplId);

        // 담보 LTV FAIL
        prepEligible(ltvFailApplId, "APPROVE", 200_000_000L, /*dsrFail*/ false);
        Long col = createCollateral(ltvFailApplId);
        // applied 30M / requested 30M → ratio 10000 > 7000 → LTV FAIL
        evaluateCollateral(col, 30_000_000L);
        runLtv(col);
    }

    @Test @Order(10)
    void 자동_APPROVED() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", approveApplId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applId").value(approveApplId))
                .andExpect(jsonPath("$.data.revTypeCd").value("AUTO"))
                .andExpect(jsonPath("$.data.revStatusCd").value("COMPLETED"))
                .andExpect(jsonPath("$.data.revDecisionCd").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedAmount").value(AMOUNT))
                .andExpect(jsonPath("$.data.approvedRateBps").value(BASE_BPS))
                .andExpect(jsonPath("$.data.approvedPeriodMo").value(MONTHS))
                .andExpect(jsonPath("$.data.approvedAt").exists())
                .andExpect(jsonPath("$.data.rejectReasonCd").doesNotExist());
    }

    @Test @Order(11)
    void 자동_REJECTED_CB_REJECT() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", cbRejectApplId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.revDecisionCd").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReasonCd").value("CB_REJECT"))
                .andExpect(jsonPath("$.data.approvedAmount").doesNotExist())
                .andExpect(jsonPath("$.data.approvedAt").doesNotExist());
    }

    @Test @Order(12)
    void 자동_REJECTED_DSR_FAIL() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", dsrFailApplId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.revDecisionCd").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReasonCd").value("DSR_OVER"));
    }

    @Test @Order(13)
    void 자동_REJECTED_LTV_FAIL_담보필수() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", ltvFailApplId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.revDecisionCd").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReasonCd").value("LTV_FAIL"));
    }

    @Test @Order(14)
    void 자동_결정_불가_CB_REVIEW_422() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", cbReviewApplId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_048"));
    }

    @Test @Order(15)
    void 본심사_이미_수행_409() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", alreadyReviewedAppl))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_039"));
    }

    @Test @Order(16)
    void 미존재_applId_404() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_012"));
    }

    @Test @Order(17)
    void CB_미수행_데이터부족_422() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review/auto-decide", noCevalApplId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_038"));
    }

    // ============================================================
    // helpers
    // ============================================================

    private String uniq() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Long createCreditProduct() throws Exception {
        String code = "AUD_C_" + uniq();
        String body = """
                {
                  "prodCd":"%s", "prodName":"자동결정 신용대출", "loanTypeCd":"CREDIT",
                  "repaymentMethodCd":"EQUAL", "rateTypeCd":"FIXED",
                  "baseRateBps":%d,
                  "minAmount":1000000, "maxAmount":100000000,
                  "minPeriodMo":12, "maxPeriodMo":60,
                  "collateralRequiredYn":"N", "guarantorRequiredYn":"N"
                }
                """.formatted(code, BASE_BPS);
        MvcResult result = mockMvc.perform(post("/api/loan-products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractData(result).get("prodId").asLong();
    }

    private Long createMortgageProduct() throws Exception {
        String code = "AUD_M_" + uniq();
        String body = """
                {
                  "prodCd":"%s", "prodName":"자동결정 담보대출", "loanTypeCd":"MORTGAGE",
                  "repaymentMethodCd":"EQUAL", "rateTypeCd":"FIXED",
                  "baseRateBps":%d,
                  "minAmount":1000000, "maxAmount":1000000000,
                  "minPeriodMo":12, "maxPeriodMo":360,
                  "collateralRequiredYn":"Y", "guarantorRequiredYn":"N"
                }
                """.formatted(code, BASE_BPS);
        MvcResult result = mockMvc.perform(post("/api/loan-products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractData(result).get("prodId").asLong();
    }

    private void activateProduct(Long prodId) throws Exception {
        mockMvc.perform(patch("/api/loan-products/{prodId}", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "prodStatusCd":"ACTIVE" }
                                """))
                .andExpect(status().isOk());
    }

    private Long createApplication(Long prodId, long customerId) throws Exception {
        String body = """
                {
                  "customerId":%d, "prodId":%d, "channelCd":"MOBILE",
                  "requestedAmount":%d, "requestedPeriodMo":%d,
                  "loanPurposeCd":"LIVING", "repaymentMethodCd":"EQUAL"
                }
                """.formatted(customerId, prodId, AMOUNT, MONTHS);
        MvcResult result = mockMvc.perform(post("/api/loan-applications")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractData(result).get("applId").asLong();
    }

    private void runPrescreening(Long applId) throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/prescreening", applId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "prescResultCd":"PASS", "estimatedScore":700 }
                                """))
                .andExpect(status().isCreated());
    }

    private void runCeval(Long applId, String decision, long evalLimit) throws Exception {
        String body = """
                {
                  "cevalEngine":"KCB", "cevalDecisionCd":"%s", "cevalScore":700,
                  "evalLimitAmount":%d
                }
                """.formatted(decision, evalLimit);
        mockMvc.perform(post("/api/loan-applications/{applId}/credit-evaluation", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void runDsr(Long applId, boolean fail) throws Exception {
        // PASS: 연소득 80M / 신규 원리금 10M → ratio 1250 < 4000
        // FAIL: 연소득 20M / 신규 원리금 15M → ratio 7500 > 4000
        String body = fail
                ? """
                  { "annualIncomeAmt":20000000, "newAnnualRepayAmt":15000000 }
                  """
                : """
                  { "annualIncomeAmt":80000000, "newAnnualRepayAmt":10000000 }
                  """;
        mockMvc.perform(post("/api/loan-applications/{applId}/dsr-calculation", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void prepEligible(Long applId, String cevalDecision, long evalLimit, boolean dsrFail)
            throws Exception {
        runPrescreening(applId);
        runCeval(applId, cevalDecision, evalLimit);
        runDsr(applId, dsrFail);
    }

    private void runReviewApprovedManually(Long applId) throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", applId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"MANUAL", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isCreated());
    }

    private Long createCollateral(Long applId) throws Exception {
        String body = """
                {
                  "colTypeCd":"REAL_ESTATE", "colName":"자동결정 담보",
                  "declaredValue":50000000, "currencyCd":"KRW", "ownershipTypeCd":"SOLE",
                  "seniorLienYn":"N", "seniorLienAmount":0
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/loan-applications/{applId}/collaterals", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractData(result).get("colId").asLong();
    }

    private void evaluateCollateral(Long colId, long appliedValue) throws Exception {
        String body = """
                {
                  "evalMethodCd":"APPRAISAL", "evalAgencyCd":"KAB",
                  "appraisedValue":%d, "appliedValue":%d,
                  "appliedStartDate":"20260101", "appliedEndDate":"20271231"
                }
                """.formatted(appliedValue, appliedValue);
        mockMvc.perform(post("/api/collaterals/{colId}/evaluations", colId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void runLtv(Long colId) throws Exception {
        mockMvc.perform(post("/api/collaterals/{colId}/ltv-calculation", colId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }
}
