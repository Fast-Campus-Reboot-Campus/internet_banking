package com.bank.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 자행 동기 흐름 통합테스트 (S1/F1/계좌폐쇄).
 *
 * mock 프로파일 기준 트리거 계좌:
 *   SENDER_S1   "12345678901234" — 이몽룡, 잔액 20억, 정상 계좌
 *   RECEIVER_S1 "12345678905678" — 성춘향, 정상 계좌
 *   SENDER_F1   "77770000000001" — mock '기타' 계좌, 잔액 500만
 *   RECEIVER_CLOSED "99990000000003" — accountStatus=CLOSED → ACCOUNT_CLOSED
 *
 * 자행 은행코드: A은행 → BankCodeMapper.toNumeric("A") = "004"
 * 600만(6_000_000) 이체 요청 시 500만 잔액 < 600만 필요액 → INSUFFICIENT_BALANCE
 */
class PaymentSyncFlowTest extends AbstractPaymentIntegrationTest {

    private static final String BANK_CODE_A      = "004";
    private static final String SENDER_S1        = "12345678901234";
    private static final String RECEIVER_S1      = "12345678905678";
    private static final String SENDER_F1        = "77770000000001";
    private static final String RECEIVER_CLOSED  = "99990000000003";

    @Test
    @DisplayName("S1 자행이체 정상 — 200, status=COMPLETED, ledger 2건")
    void s1_intraBankNormal_completed() throws Exception {
        MvcResult result = mockMvc.perform(postPayment(
                "PAY-S1-001-1", "USER-001", "AUTH-001",
                SENDER_S1, BANK_CODE_A, RECEIVER_S1, "성춘향",
                100_000L, "MOBILE"
        ))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andReturn();

        String piId = om.readTree(result.getResponse().getContentAsString())
                .get("paymentInstructionId").asText();

        String dbStatus = jdbc.queryForObject(
                "SELECT status FROM payment_instruction WHERE payment_instruction_id = ?",
                String.class, piId);
        assertThat(dbStatus).isEqualTo("COMPLETED");

        int ledgerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledger WHERE payment_instruction_id = ?",
                Integer.class, piId);
        assertThat(ledgerCount).isEqualTo(2);
    }

    @Test
    @DisplayName("F1 잔액부족 직행 — 200, status=FAILED, failureCategory=INSUFFICIENT_BALANCE, ledger 0건")
    void f1_insufficientBalance_failed() throws Exception {
        // SENDER_F1 잔액 500만(mock '기타'), 이체금액 600만 → B-1 잔액체크 실패
        MvcResult result = mockMvc.perform(postPayment(
                "PAY-F1-001-1", "USER-002", "AUTH-002",
                SENDER_F1, BANK_CODE_A, RECEIVER_S1, "성춘향",
                6_000_000L, "MOBILE"
        ))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.failureCategory").value("INSUFFICIENT_BALANCE"))
        .andReturn();

        String piId = om.readTree(result.getResponse().getContentAsString())
                .get("paymentInstructionId").asText();

        String dbStatus = jdbc.queryForObject(
                "SELECT status FROM payment_instruction WHERE payment_instruction_id = ?",
                String.class, piId);
        assertThat(dbStatus).isEqualTo("FAILED");

        String dbFailureCategory = jdbc.queryForObject(
                "SELECT failure_category FROM payment_instruction WHERE payment_instruction_id = ?",
                String.class, piId);
        assertThat(dbFailureCategory).isEqualTo("INSUFFICIENT_BALANCE");

        int ledgerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledger WHERE payment_instruction_id = ?",
                Integer.class, piId);
        assertThat(ledgerCount).isEqualTo(0);
    }

    @Test
    @DisplayName("계좌폐쇄 — 200, status=FAILED, failureCategory=ACCOUNT_CLOSED, ACCOUNT_CHECK_FAILED 이벤트 1건")
    void f_accountClosed_failed() throws Exception {
        // RECEIVER_CLOSED: accountStatus=CLOSED → A-1 수신계좌 체크에서 ACCOUNT_CLOSED throw
        MvcResult result = mockMvc.perform(postPayment(
                "PAY-CLOSED-001-1", "USER-003", "AUTH-003",
                SENDER_S1, BANK_CODE_A, RECEIVER_CLOSED, "홍길동",
                100_000L, "MOBILE"
        ))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.failureCategory").value("ACCOUNT_CLOSED"))
        .andReturn();

        String piId = om.readTree(result.getResponse().getContentAsString())
                .get("paymentInstructionId").asText();

        String dbStatus = jdbc.queryForObject(
                "SELECT status FROM payment_instruction WHERE payment_instruction_id = ?",
                String.class, piId);
        assertThat(dbStatus).isEqualTo("FAILED");

        int eventCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM status_history " +
                "WHERE payment_instruction_id = ? AND event_type = 'ACCOUNT_CHECK_FAILED'",
                Integer.class, piId);
        assertThat(eventCount).isEqualTo(1);
    }
}
