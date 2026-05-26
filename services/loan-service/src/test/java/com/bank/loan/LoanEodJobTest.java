package com.bank.loan;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.support.AbstractLoanIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LoanEodJob 통합 테스트 (연도: 2035 — 다른 배치 테스트와 날짜 충돌 방지).
 *
 * 세팅 (cntrStartDate=20350101 → 회차1 due_date=20350201):
 *   - 계약 A: auto_debit_yn=Y, VERIFIED → 자동이체 처리 대상
 *   - 계약 B: auto_debit_yn=N          → 자동이체 skip, 미납 시 연체 전환 대상
 *
 * 시나리오:
 *   10) EOD baseDate=20350201 (납기일) → COMPLETED
 *   11) A 회차1=PAID, B 회차1=DUE 확인
 *   12) A·B 이자발생 행 존재 확인
 *   13) 연체 아직 없음 (납기일 당일은 OVERDUE 아님)
 *   20) EOD baseDate=20350205 (납기일+4일) → COMPLETED
 *   21) B 회차1=OVERDUE
 *   22) B 연체 ACTIVE, dlqDays=4, STAGE_0
 *   23) A 연체 없음 (납기일에 이미 PAID)
 *   30) 동일 baseDate=20350201 재실행 → SKIPPED (JobInstanceAlreadyComplete)
 *   40) baseDate 형식 오류 → 400
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanEodJobTest extends AbstractLoanIntegrationTest {

    @Autowired
    private LoanApplicationRepository applicationRepository;

    private static final String CNTR_START_DATE = "20350101";
    private static final String EOD_DUE_DATE    = "20350201";  // 납기일 당일
    private static final String EOD_OVERDUE     = "20350205";  // 납기일 +4일 → dlqDays=4, STAGE_0

    private static final long CONTRACTED_AMOUNT = 12_000_000L;
    private static final int  PERIOD_MONTHS     = 12;
    private static final int  RATE_BPS          = 600;

    private Long cntrIdA;
    private Long cntrIdB;

    @BeforeAll
    void setup() throws Exception {
        cntrIdA = setupContract("Y");
        cntrIdB = setupContract("N");
    }

    // ────────────────────────────────────────────
    // Phase 1: EOD on due date
    // ────────────────────────────────────────────

    @Test @Order(10)
    void EOD_납기일_실행_COMPLETED() throws Exception {
        mockMvc.perform(post("/api/internal/eod/run").param("baseDate", EOD_DUE_DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.baseDate").value(EOD_DUE_DATE));
    }

    @Test @Order(11)
    void A_회차1_PAID_B_회차1_DUE() throws Exception {
        mockMvc.perform(get("/api/loan-contracts/{cntrId}/repayment-schedules", cntrIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].rschStatusCd").value("PAID"));

        mockMvc.perform(get("/api/loan-contracts/{cntrId}/repayment-schedules", cntrIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].rschStatusCd").value("DUE"));
    }

    @Test @Order(12)
    void A_B_이자발생_행_존재() throws Exception {
        mockMvc.perform(get("/api/loan-contracts/{cntrId}/interest-accruals", cntrIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].accrualDate").value(EOD_DUE_DATE));

        mockMvc.perform(get("/api/loan-contracts/{cntrId}/interest-accruals", cntrIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].accrualDate").value(EOD_DUE_DATE));
    }

    @Test @Order(13)
    void 납기일_당일_연체_없음() throws Exception {
        mockMvc.perform(get("/api/loan-contracts/{cntrId}/delinquency", cntrIdB))
                .andExpect(status().isNotFound());
    }

    // ────────────────────────────────────────────
    // Phase 2: EOD 4 days past due → delinquency
    // ────────────────────────────────────────────

    @Test @Order(20)
    void EOD_연체일_실행_COMPLETED() throws Exception {
        mockMvc.perform(post("/api/internal/eod/run").param("baseDate", EOD_OVERDUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.baseDate").value(EOD_OVERDUE));
    }

    @Test @Order(21)
    void B_회차1_OVERDUE() throws Exception {
        mockMvc.perform(get("/api/loan-contracts/{cntrId}/repayment-schedules", cntrIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].rschStatusCd").value("OVERDUE"));
    }

    @Test @Order(22)
    void B_연체_ACTIVE_dlqDays_4_STAGE_0() throws Exception {
        mockMvc.perform(get("/api/loan-contracts/{cntrId}/delinquency", cntrIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dlqStatusCd").value("ACTIVE"))
                .andExpect(jsonPath("$.data.dlqDays").value(4))
                .andExpect(jsonPath("$.data.dlqStageCd").value("STAGE_0"));
    }

    @Test @Order(23)
    void A_연체_없음_납기일에_이미_상환() throws Exception {
        mockMvc.perform(get("/api/loan-contracts/{cntrId}/delinquency", cntrIdA))
                .andExpect(status().isNotFound());
    }

    // ────────────────────────────────────────────
    // Phase 3: 멱등성 + 유효성 검사
    // ────────────────────────────────────────────

    @Test @Order(30)
    void 동일_baseDate_재실행_SKIPPED() throws Exception {
        mockMvc.perform(post("/api/internal/eod/run").param("baseDate", EOD_DUE_DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobStatus").value("SKIPPED"));
    }

    @Test @Order(40)
    void baseDate_형식오류_400() throws Exception {
        mockMvc.perform(post("/api/internal/eod/run").param("baseDate", "2035-02-01"))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────────────────────

    private Long setupContract(String autoDebitYn) throws Exception {
        Long prodId = createProduct();
        activateProduct(prodId);
        Long applId = createApplication(prodId);
        forceApprove(applId);
        Long cntrId = createContract(applId);
        registerAndVerifyRepaymentAccount(cntrId, autoDebitYn);
        triggerDrawdown(cntrId, CONTRACTED_AMOUNT);
        return cntrId;
    }

    private Long createProduct() throws Exception {
        String code = "EOD_" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {
                  "prodCd":"%s","prodName":"EOD 테스트 상품","loanTypeCd":"CREDIT",
                  "repaymentMethodCd":"EQUAL","rateTypeCd":"FIXED",
                  "baseRateBps":600,
                  "minAmount":1000000,"maxAmount":100000000,
                  "minPeriodMo":12,"maxPeriodMo":60,
                  "collateralRequiredYn":"N","guarantorRequiredYn":"N"
                }
                """.formatted(code);
        MvcResult r = mockMvc.perform(post("/api/loan-products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return extractData(r).get("prodId").asLong();
    }

    private void activateProduct(Long prodId) throws Exception {
        mockMvc.perform(patch("/api/loan-products/{prodId}", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{ "prodStatusCd":"ACTIVE" }"""))
                .andExpect(status().isOk());
    }

    private Long createApplication(Long prodId) throws Exception {
        String body = """
                {
                  "customerId":7001,"prodId":%d,"channelCd":"MOBILE",
                  "requestedAmount":%d,"requestedPeriodMo":%d,
                  "loanPurposeCd":"LIVING","repaymentMethodCd":"EQUAL"
                }
                """.formatted(prodId, CONTRACTED_AMOUNT, PERIOD_MONTHS);
        MvcResult r = mockMvc.perform(post("/api/loan-applications")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return extractData(r).get("applId").asLong();
    }

    private void forceApprove(Long applId) {
        LoanApplication app = applicationRepository.findByApplIdAndDeletedAtIsNull(applId).orElseThrow();
        app.markApproved();
        applicationRepository.save(app);
    }

    private Long createContract(Long applId) throws Exception {
        String body = """
                {
                  "applId":%d,
                  "contractedAmount":%d,
                  "contractedPeriodMo":%d,
                  "baseRateBps":%d,
                  "rateTypeCd":"FIXED",
                  "repaymentMethodCd":"EQUAL",
                  "cntrStartDate":"%s"
                }
                """.formatted(applId, CONTRACTED_AMOUNT, PERIOD_MONTHS, RATE_BPS, CNTR_START_DATE);
        MvcResult r = mockMvc.perform(post("/api/loan-contracts")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return extractData(r).get("cntrId").asLong();
    }

    private void registerAndVerifyRepaymentAccount(Long cntrId, String autoDebitYn) throws Exception {
        String body = """
                { "bankCd":"088","accountNo":"1102345678901",
                  "holderName":"홍길동","autoDebitYn":"%s","debitDay":1 }
                """.formatted(autoDebitYn);
        mockMvc.perform(post("/api/loan-contracts/{cntrId}/repayment-account", cntrId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/loan-contracts/{cntrId}/repayment-account/verify", cntrId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    private void triggerDrawdown(Long cntrId, long amount) throws Exception {
        mockMvc.perform(post("/api/loan-contracts/{cntrId}/executions", cntrId)
                        .header("Idempotency-Key", "eod-drawdown-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executedAmount":%d,
                                  "disbursementBankCd":"088",
                                  "disbursementAccountNo":"1109999998888"
                                }
                                """.formatted(amount)))
                .andExpect(status().isCreated());
    }
}
