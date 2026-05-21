package com.bank.loan;

import com.bank.loan.support.AbstractLoanIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 본심사 통합 테스트.
 *
 * 시나리오:
 *   10) APPROVED — CB+DSR 모두 통과 신청, 한도/금리/기간 자동 산정 + approvedAt 기록
 *   11) 동일 신청 재본심사 차단 → 409 LOAN_039
 *   12) GET 단건 조회 OK
 *   13) REJECTED — 수동 거절, rejectReasonCd 기록
 *   14) CB 미수행 신청 본심사 시도 → 422 LOAN_038
 *   15) CB REJECT 신청 본심사 시도 → 422 LOAN_038
 *   16) DSR FAIL 신청 본심사 시도 → 422 LOAN_038
 *   17) PRESCREENED 아닌 신청(SUBMITTED) 본심사 시도 → 422 LOAN_038
 *   18) GET 미존재 신청 → 404 LOAN_012
 *   19) GET 본심사 안 한 신청 → 404 LOAN_042
 *   20) revDecisionCd 누락 → 400
 *   21) APPROVED + 입력 override (한도/금리/기간 명시값 그대로 사용)
 *   22) APPROVED + CB.evalLimit 가 신청금액보다 작으면 그 값으로 한도 제한
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanReviewFlowTest extends AbstractLoanIntegrationTest {

    private static final long AMOUNT  = 30_000_000L;
    private static final int  MONTHS  = 36;
    private static final int  BASE_BPS = 500;

    private Long prodId;
    private Long approveApplId;
    private Long rejectApplId;
    private Long noCevalApplId;
    private Long cevalRejectApplId;
    private Long dsrFailApplId;
    private Long submittedApplId;
    private Long pristineApplId;
    private Long overrideApplId;
    private Long capLimitApplId;

    @org.junit.jupiter.api.BeforeAll
    void setup() throws Exception {
        prodId = createProduct();
        activateProduct(prodId);

        approveApplId   = createApplication(prodId);
        rejectApplId    = createApplication(prodId);
        noCevalApplId   = createApplication(prodId);
        cevalRejectApplId = createApplication(prodId);
        dsrFailApplId   = createApplication(prodId);
        submittedApplId = createApplication(prodId);
        pristineApplId  = createApplication(prodId);
        overrideApplId  = createApplication(prodId);
        capLimitApplId  = createApplication(prodId);

        // 정상 경로: 가심사 + CB(APPROVE) + DSR(PASS)
        prepFullyEligible(approveApplId, 50_000_000L);
        prepFullyEligible(rejectApplId, 50_000_000L);
        prepFullyEligible(pristineApplId, 50_000_000L);
        prepFullyEligible(overrideApplId, 50_000_000L);
        prepFullyEligible(capLimitApplId, 10_000_000L); // CB 한도 신청금액보다 낮음

        // CB 까지만, DSR 미수행
        runPrescreening(noCevalApplId, "PASS");

        // CB REJECT
        runPrescreening(cevalRejectApplId, "PASS");
        runCeval(cevalRejectApplId, "REJECT", 50_000_000L);

        // DSR FAIL
        runPrescreening(dsrFailApplId, "PASS");
        runCeval(dsrFailApplId, "APPROVE", 50_000_000L);
        runDsrFail(dsrFailApplId);

        // submittedApplId 는 가심사도 안 함 (SUBMITTED)
    }

    @Test @Order(10)
    void APPROVED_자동산정() throws Exception {
        String body = """
                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                """;
        mockMvc.perform(post("/api/loan-applications/{applId}/review", approveApplId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applId").value(approveApplId))
                .andExpect(jsonPath("$.data.revStatusCd").value("COMPLETED"))
                .andExpect(jsonPath("$.data.revDecisionCd").value("APPROVED"))
                // CB.evalLimit=50M >= req=30M, product max=100M → approved=30M
                .andExpect(jsonPath("$.data.approvedAmount").value(AMOUNT))
                .andExpect(jsonPath("$.data.approvedRateBps").value(BASE_BPS))
                .andExpect(jsonPath("$.data.approvedPeriodMo").value(MONTHS))
                .andExpect(jsonPath("$.data.approvedAt").exists());
    }

    @Test @Order(11)
    void 동일_신청_재본심사_차단_409() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", approveApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_039"));
    }

    @Test @Order(12)
    void GET_단건_조회() throws Exception {
        mockMvc.perform(get("/api/loan-applications/{applId}/review", approveApplId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applId").value(approveApplId))
                .andExpect(jsonPath("$.data.revDecisionCd").value("APPROVED"));
    }

    @Test @Order(13)
    void REJECTED_수동_거절() throws Exception {
        String body = """
                {
                  "revTypeCd":"MANUAL",
                  "revDecisionCd":"REJECTED",
                  "rejectReasonCd":"POLICY_VIOLATION",
                  "revRemark":"내부 정책 위반",
                  "reviewerId":99001
                }
                """;
        mockMvc.perform(post("/api/loan-applications/{applId}/review", rejectApplId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.revDecisionCd").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReasonCd").value("POLICY_VIOLATION"))
                .andExpect(jsonPath("$.data.reviewerId").value(99001))
                .andExpect(jsonPath("$.data.approvedAmount").doesNotExist())
                .andExpect(jsonPath("$.data.approvedAt").doesNotExist());
    }

    @Test @Order(14)
    void CB_미수행_사전조건_미충족_422() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", noCevalApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_038"));
    }

    @Test @Order(15)
    void CB_REJECT_사전조건_미충족_422() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", cevalRejectApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_038"));
    }

    @Test @Order(16)
    void DSR_FAIL_사전조건_미충족_422() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", dsrFailApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_038"));
    }

    @Test @Order(17)
    void PRESCREENED_아님_SUBMITTED_상태_422() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", submittedApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOAN_038"));
    }

    @Test @Order(18)
    void GET_미존재_신청_404() throws Exception {
        mockMvc.perform(get("/api/loan-applications/{applId}/review", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_012"));
    }

    @Test @Order(19)
    void GET_본심사_안한_신청_404() throws Exception {
        mockMvc.perform(get("/api/loan-applications/{applId}/review", pristineApplId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_042"));
    }

    @Test @Order(20)
    void revDecisionCd_누락_400() throws Exception {
        mockMvc.perform(post("/api/loan-applications/{applId}/review", pristineApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(21)
    void APPROVED_입력_override() throws Exception {
        String body = """
                {
                  "revTypeCd":"MANUAL",
                  "revDecisionCd":"APPROVED",
                  "approvedAmount":20000000,
                  "approvedRateBps":420,
                  "approvedPeriodMo":24,
                  "reviewerId":99002
                }
                """;
        mockMvc.perform(post("/api/loan-applications/{applId}/review", overrideApplId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.approvedAmount").value(20000000))
                .andExpect(jsonPath("$.data.approvedRateBps").value(420))
                .andExpect(jsonPath("$.data.approvedPeriodMo").value(24));
    }

    @Test @Order(22)
    void APPROVED_CB_한도로_제한() throws Exception {
        // CB.evalLimit=10M < req=30M → approved=10M
        mockMvc.perform(post("/api/loan-applications/{applId}/review", capLimitApplId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "revTypeCd":"AUTO", "revDecisionCd":"APPROVED" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.approvedAmount").value(10000000));
    }

    // ============================================================
    // helpers
    // ============================================================

    private String uniq() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Long createProduct() throws Exception {
        String code = "REV_" + uniq();
        String body = """
                {
                  "prodCd":"%s", "prodName":"본심사 테스트", "loanTypeCd":"CREDIT",
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

    private void activateProduct(Long prodId) throws Exception {
        mockMvc.perform(patch("/api/loan-products/{prodId}", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "prodStatusCd":"ACTIVE" }
                                """))
                .andExpect(status().isOk());
    }

    private Long createApplication(Long prodId) throws Exception {
        String body = """
                {
                  "customerId":8001, "prodId":%d, "channelCd":"MOBILE",
                  "requestedAmount":%d, "requestedPeriodMo":%d,
                  "loanPurposeCd":"LIVING", "repaymentMethodCd":"EQUAL"
                }
                """.formatted(prodId, AMOUNT, MONTHS);
        MvcResult result = mockMvc.perform(post("/api/loan-applications")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractData(result).get("applId").asLong();
    }

    private void runPrescreening(Long applId, String result) throws Exception {
        String body = "PASS".equals(result)
                ? """
                  { "prescResultCd":"PASS", "estimatedGrade":"BBB", "estimatedScore":700 }
                  """
                : """
                  { "prescResultCd":"REJECT", "rejectReasonCd":"LOW_INCOME" }
                  """;
        mockMvc.perform(post("/api/loan-applications/{applId}/prescreening", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void runCeval(Long applId, String decisionCd, long evalLimit) throws Exception {
        String body = """
                {
                  "cevalEngine":"KCB",
                  "cevalDecisionCd":"%s",
                  "cevalScore":700,
                  "evalLimitAmount":%d
                }
                """.formatted(decisionCd, evalLimit);
        mockMvc.perform(post("/api/loan-applications/{applId}/credit-evaluation", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void runDsrPass(Long applId) throws Exception {
        // 연소득 80M, 신규 연 원리금 10M → ratio=1250 < 4000 → PASS
        String body = """
                {
                  "annualIncomeAmt":80000000,
                  "newAnnualRepayAmt":10000000
                }
                """;
        mockMvc.perform(post("/api/loan-applications/{applId}/dsr-calculation", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void runDsrFail(Long applId) throws Exception {
        // 연소득 20M, 신규 연 원리금 15M → ratio=7500 > 4000 → FAIL
        String body = """
                {
                  "annualIncomeAmt":20000000,
                  "newAnnualRepayAmt":15000000
                }
                """;
        mockMvc.perform(post("/api/loan-applications/{applId}/dsr-calculation", applId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void prepFullyEligible(Long applId, long cevalLimit) throws Exception {
        runPrescreening(applId, "PASS");
        runCeval(applId, "APPROVE", cevalLimit);
        runDsrPass(applId);
    }
}
