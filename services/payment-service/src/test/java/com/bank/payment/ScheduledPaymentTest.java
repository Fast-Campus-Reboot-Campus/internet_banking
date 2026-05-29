package com.bank.payment;

import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.domain.mapper.PaymentInstructionMapper;
import com.bank.payment.domain.service.PaymentTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired private PaymentTransactionService txService;
    @Autowired private PaymentInstructionMapper paymentInstructionMapper;

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

    // ── claim 단계 테스트 (단계 2) ─────────────────────────────────────────

    /**
     * 헬퍼: 예약이체 등록 후 piId 반환 (SCHEDULED 상태).
     */
    private String registerScheduled(String idKey, String userId, String authId,
                                     LocalDateTime scheduledAt) throws Exception {
        MvcResult result = mockMvc.perform(postScheduledPayment(
                idKey, userId, authId,
                SENDER_S1, BANK_CODE_A, RECEIVER_S1, "성춘향",
                100_000L, "MOBILE", scheduledAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString())
                .get("paymentInstructionId").asText();
    }

    @Test
    @DisplayName("sched_claim_single — SCHEDULED PI claim → true, status=PROCESSING, version+1, SCHEDULED_TRIGGERED 이력, ledger/outbox 0건")
    void sched_claim_single() throws Exception {
        // 1. 예약 등록 (scheduled_execution_at = 미래)
        String piId = registerScheduled("SCHED-CLM-001-1", "USER-CLM-001", "AUTH-CLM-001",
                LocalDateTime.now().plusHours(1));

        // 2. scheduled_execution_at 을 과거로 설정 → 워커가 집을 수 있는 상태로 만듦
        jdbc.update("UPDATE payment_instruction SET scheduled_execution_at = ? " +
                    "WHERE payment_instruction_id = ?",
                LocalDateTime.now().minusMinutes(1), piId);

        // 3. selectDueScheduled 로 PI 읽기 (워커 폴링 대신 결정적 직접 호출)
        List<PaymentInstruction> due = paymentInstructionMapper.selectDueScheduled();
        PaymentInstruction pi = due.stream()
                .filter(p -> piId.equals(p.getPaymentInstructionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("selectDueScheduled 결과에 piId 없음: " + piId));

        int versionBeforeClaim = pi.getVersion();

        // 4. claim 직접 호출
        boolean claimed = txService.claimScheduled(pi);

        // 5. 검증
        assertThat(claimed).isTrue();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, version FROM payment_instruction WHERE payment_instruction_id = ?", piId);
        assertThat(row.get("status")).isEqualTo("PROCESSING");
        assertThat(((Number) row.get("version")).intValue()).isEqualTo(versionBeforeClaim + 1);

        // SCHEDULED_TRIGGERED 이력 1건, triggered_by=SCHEDULER
        int trigCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM status_history " +
                "WHERE payment_instruction_id = ? AND event_type = 'SCHEDULED_TRIGGERED' " +
                "AND triggered_by = 'SCHEDULER'",
                Integer.class, piId);
        assertThat(trigCount).isEqualTo(1);

        // 이전 상태 = SCHEDULED, 다음 상태 = PROCESSING 확인
        Map<String, Object> hist = jdbc.queryForMap(
                "SELECT previous_status, next_status FROM status_history " +
                "WHERE payment_instruction_id = ? AND event_type = 'SCHEDULED_TRIGGERED'", piId);
        assertThat(hist.get("previous_status")).isEqualTo("SCHEDULED");
        assertThat(hist.get("next_status")).isEqualTo("PROCESSING");

        // ledger 0건, outbox 0건 (실행 단계 미진입)
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledger WHERE payment_instruction_id = ?",
                Integer.class, piId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_message WHERE payment_instruction_id = ?",
                Integer.class, piId)).isZero();
    }

    @Test
    @DisplayName("sched_claim_double_idempotent — 이중선점 방지: 1회차 true, 2회차 false, DB status=PROCESSING 1회·이력 1건")
    void sched_claim_double_idempotent() throws Exception {
        // 1. 예약 등록 후 과거 시각으로 설정
        String piId = registerScheduled("SCHED-CLM-002-1", "USER-CLM-002", "AUTH-CLM-002",
                LocalDateTime.now().plusHours(1));
        jdbc.update("UPDATE payment_instruction SET scheduled_execution_at = ? " +
                    "WHERE payment_instruction_id = ?",
                LocalDateTime.now().minusMinutes(1), piId);

        // 2. selectDueScheduled 로 PI 획득 (version 고정)
        PaymentInstruction pi = paymentInstructionMapper.selectDueScheduled().stream()
                .filter(p -> piId.equals(p.getPaymentInstructionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("PI 없음: " + piId));

        // 3. 1회차 claim
        boolean first = txService.claimScheduled(pi);
        assertThat(first).isTrue();

        // 4. 동일 pi 객체(version 변경 없음)로 2회차 claim → version 불일치로 false
        boolean second = txService.claimScheduled(pi);
        assertThat(second).isFalse();

        // 5. DB: status=PROCESSING, version은 1회만 올라감
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, version FROM payment_instruction WHERE payment_instruction_id = ?", piId);
        assertThat(row.get("status")).isEqualTo("PROCESSING");
        assertThat(((Number) row.get("version")).intValue()).isEqualTo(pi.getVersion() + 1);

        // SCHEDULED_TRIGGERED 이력 정확히 1건 (중복 INSERT 없음)
        int trigCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM status_history " +
                "WHERE payment_instruction_id = ? AND event_type = 'SCHEDULED_TRIGGERED'",
                Integer.class, piId);
        assertThat(trigCount).isEqualTo(1);
    }

    @Test
    @DisplayName("sched_claim_notDue — scheduled_execution_at 미래인 PI는 selectDueScheduled 에 안 잡힘(0건)")
    void sched_claim_notDue() throws Exception {
        // 1. 예약 등록 (미래 시각 — 과거로 바꾸지 않음)
        String piId = registerScheduled("SCHED-CLM-003-1", "USER-CLM-003", "AUTH-CLM-003",
                LocalDateTime.now().plusHours(2));

        // 2. selectDueScheduled 결과에 해당 PI 없음
        List<PaymentInstruction> due = paymentInstructionMapper.selectDueScheduled();
        boolean found = due.stream().anyMatch(p -> piId.equals(p.getPaymentInstructionId()));
        assertThat(found).isFalse();

        // 3. DB status 여전히 SCHEDULED
        String status = jdbc.queryForObject(
                "SELECT status FROM payment_instruction WHERE payment_instruction_id = ?",
                String.class, piId);
        assertThat(status).isEqualTo("SCHEDULED");
    }
}
