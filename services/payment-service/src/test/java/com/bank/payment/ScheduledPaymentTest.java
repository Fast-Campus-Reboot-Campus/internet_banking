package com.bank.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약이체 등록 통합테스트 (등록경로 DRAFT→AUTHORIZED→SCHEDULED).
 * 워커/실행/PROCESSING 전이/취소/Outbox/ledger 검증 없음 — 이 PR 범위 외.
 *
 * mock 프로파일 트리거 계좌:
 *   SENDER_S1       "12345678901234" — 정상 계좌, 잔액 20억
 *   RECEIVER_S1     "12345678905678" — 정상 계좌
 *   RECEIVER_CLOSED "99990000000003" — accountStatus=CLOSED → ACCOUNT_CLOSED
 */
class ScheduledPaymentTest extends AbstractPaymentIntegrationTest {

    private static final String BANK_CODE_A      = "004";
    private static final String SENDER_S1        = "12345678901234";
    private static final String RECEIVER_S1      = "12345678905678";
    private static final String RECEIVER_CLOSED  = "99990000000003";

    private MockHttpServletRequestBuilder postScheduledPayment(
            String idempotencyKey,
            String userId,
            String authTokenId,
            String senderAccountId,
            String receiverBankCode,
            String receiverAccountNo,
            String receiverHolderName,
            long transferAmount,
            String channel,
            LocalDateTime scheduledExecutionAt) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("senderAccountId",               senderAccountId);
        body.put("receiverBankCode",              receiverBankCode);
        body.put("receiverAccountNo",             receiverAccountNo);
        body.put("receiverHolderName",            receiverHolderName);
        body.put("transferAmount",                BigDecimal.valueOf(transferAmount));
        body.put("receiverMemo",                  "예약이체");
        body.put("senderMemo",                    "예약송금");
        body.put("channel",                       channel);
        body.put("receiverPassbookSenderDisplay", "이몽룡");
        body.put("scheduledExecutionAt",          scheduledExecutionAt);

        return post("/api/v1/payments/scheduled")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-User-Id",          userId)
                .header("X-Auth-Token-Id",    authTokenId)
                .content(om.writeValueAsString(body));
    }

    @Test
    @DisplayName("sX 자행 예약등록 정상 — SCHEDULED, ledger 0건, outbox 0건, AUTH_PASSED+SCHEDULED_REGISTERED 이력")
    void sX_registerScheduled_scheduled() throws Exception {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(2);

        MvcResult result = mockMvc.perform(postScheduledPayment(
                "SCHED-001-1", "USER-001", "AUTH-001",
                SENDER_S1, BANK_CODE_A, RECEIVER_S1, "성춘향",
                100_000L, "MOBILE", futureTime
        ))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SCHEDULED"))
        .andReturn();

        String piId = om.readTree(result.getResponse().getContentAsString())
                .get("paymentInstructionId").asText();

        // payment_instruction 상태 검증
        Map<String, Object> pi = jdbc.queryForMap(
                "SELECT status, is_scheduled, scheduled_execution_at, trigger_source, " +
                "receiver_holder_name_snap, auth_token_id " +
                "FROM payment_instruction WHERE payment_instruction_id = ?", piId);
        assertThat(pi.get("status")).isEqualTo("SCHEDULED");
        assertThat(pi.get("is_scheduled")).isEqualTo(true);
        assertThat(pi.get("scheduled_execution_at")).isNotNull();
        assertThat(pi.get("trigger_source")).isEqualTo("USER");
        assertThat(pi.get("receiver_holder_name_snap")).isNotNull();
        assertThat(pi.get("auth_token_id")).isEqualTo("AUTH-001");

        // ledger 0건 (PROCESSING 진입 안 함)
        int ledgerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledger WHERE payment_instruction_id = ?",
                Integer.class, piId);
        assertThat(ledgerCount).isZero();

        // outbox 0건
        int outboxCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_message WHERE payment_instruction_id = ?",
                Integer.class, piId);
        assertThat(outboxCount).isZero();

        // status_history: AUTH_PASSED 1건 + SCHEDULED_REGISTERED 1건
        int authPassedCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM status_history " +
                "WHERE payment_instruction_id = ? AND event_type = 'AUTH_PASSED'",
                Integer.class, piId);
        assertThat(authPassedCount).isEqualTo(1);

        int scheduledRegisteredCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM status_history " +
                "WHERE payment_instruction_id = ? AND event_type = 'SCHEDULED_REGISTERED' " +
                "AND triggered_by = 'USER'",
                Integer.class, piId);
        assertThat(scheduledRegisteredCount).isEqualTo(1);
    }

    @Test
    @DisplayName("f_register_pastTime_400 — 과거 시각 → 400, payment_instruction 0건")
    void f_register_pastTime_400() throws Exception {
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);

        mockMvc.perform(postScheduledPayment(
                "SCHED-PAST-001-1", "USER-002", "AUTH-002",
                SENDER_S1, BANK_CODE_A, RECEIVER_S1, "성춘향",
                100_000L, "MOBILE", pastTime
        ))
        .andExpect(status().isBadRequest());

        int piCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_instruction", Integer.class);
        assertThat(piCount).isZero();
    }

    @Test
    @DisplayName("f_register_accountClosed_failed — 수신계좌 폐쇄 → A 검증 실패 FAILED, ACCOUNT_CHECK_FAILED 이벤트")
    void f_register_accountClosed_failed() throws Exception {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);

        MvcResult result = mockMvc.perform(postScheduledPayment(
                "SCHED-CLOSED-001-1", "USER-003", "AUTH-003",
                SENDER_S1, BANK_CODE_A, RECEIVER_CLOSED, "홍길동",
                100_000L, "MOBILE", futureTime
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

        // ledger 0건 (자금변동 없음)
        int ledgerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledger WHERE payment_instruction_id = ?",
                Integer.class, piId);
        assertThat(ledgerCount).isZero();
    }
}
