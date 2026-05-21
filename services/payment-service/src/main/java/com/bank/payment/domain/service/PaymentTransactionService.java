package com.bank.payment.domain.service;

import com.bank.payment.common.IdGenerator;
import com.bank.payment.common.exception.LedgerBalanceMismatchException;
import com.bank.payment.domain.ExternalCall;
import com.bank.payment.domain.IdempotencyKey;
import com.bank.payment.domain.Ledger;
import com.bank.payment.domain.OutboxMessage;
import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.domain.StatusHistory;
import com.bank.payment.domain.mapper.ExternalCallMapper;
import com.bank.payment.domain.mapper.IdempotencyKeyMapper;
import com.bank.payment.domain.mapper.LedgerMapper;
import com.bank.payment.domain.mapper.OutboxMessageMapper;
import com.bank.payment.domain.mapper.PaymentInstructionMapper;
import com.bank.payment.domain.mapper.StatusHistoryMapper;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * P-028 짧은 DB 트랜잭션 격리. Orchestrator(흐름)가 외부호출 사이사이 이 메서드들을 호출.
 * 외부호출(Feign)은 절대 이 클래스에 없음 — 트랜잭션 밖(Orchestrator) 책임.
 */
@Service
public class PaymentTransactionService {

    private final PaymentInstructionMapper paymentInstructionMapper;
    private final IdempotencyKeyMapper idempotencyKeyMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final ExternalCallMapper externalCallMapper;
    private final IdGenerator idGenerator;
    private final LedgerMapper ledgerMapper;
    private final OutboxMessageMapper outboxMessageMapper;
    private final ObjectMapper objectMapper;

    public PaymentTransactionService(
            PaymentInstructionMapper paymentInstructionMapper,
            IdempotencyKeyMapper idempotencyKeyMapper,
            StatusHistoryMapper statusHistoryMapper,
            ExternalCallMapper externalCallMapper,
            IdGenerator idGenerator,
            LedgerMapper ledgerMapper,
            OutboxMessageMapper outboxMessageMapper,
            ObjectMapper objectMapper) {
        this.paymentInstructionMapper = paymentInstructionMapper;
        this.idempotencyKeyMapper = idempotencyKeyMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.externalCallMapper = externalCallMapper;
        this.idGenerator = idGenerator;
        this.ledgerMapper = ledgerMapper;
        this.outboxMessageMapper = outboxMessageMapper;
        this.objectMapper = objectMapper;
    }

    /** TX-1: 멱등키(PROCESSING) + 결제지시(DRAFT) + 상태이력(seq1 INSTRUCTION_CREATED) INSERT */
    @Transactional
    public PaymentInstruction txStep1(PaymentCommand command, boolean isIntraBank, String routingNetworkType) {
        LocalDateTime now = LocalDateTime.now();

        IdempotencyKey idempotencyKey = IdempotencyKey.of(
                command.idempotencyKey(),
                command.userId(),
                "",                     // requestHash: S1 미사용. 운영 시 요청 본문 해시
                now, now,
                now.plusMinutes(5));    // expiresAt: 합의서 일반 호출 5분 TTL
        idempotencyKeyMapper.insert(idempotencyKey);

        String piId = idGenerator.nextPaymentInstructionId();
        PaymentInstruction pi = PaymentInstruction.builder()
                .paymentInstructionId(piId)
                .idempotencyKey(command.idempotencyKey())
                .senderUserId(command.userId())
                .senderAccountId(command.senderAccountId())
                .authTokenId(command.authTokenId())
                .transactionNo("TXN-" + piId)               // 정식 채번 메서드 없음 — piId 파생 (TODO)
                .senderAccountNoSnap(command.senderAccountId()) // S1: 계좌ID=계좌번호 단순화
                .receiverBankCode(command.receiverBankCode())
                .receiverAccountNo(command.receiverAccountNo())
                .isIntraBank(isIntraBank)
                .routingNetworkType(routingNetworkType)
                .transferAmount(command.transferAmount())
                .feeAmount(BigDecimal.ZERO)                  // 자행 수수료 0
                .receiverPassbookSenderDisplay(command.receiverPassbookSenderDisplay())
                .receiverMemo(command.receiverMemo())
                .senderMemo(command.senderMemo())
                .status("DRAFT")
                .channel(command.channel())
                .requestedAt(now)
                .businessDate(now.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE))
                .version(0)
                .triggerSource("USER")
                .isScheduled(false)
                .firstRegistrantId(command.userId())
                .lastModifierId(command.userId())
                .build();
        paymentInstructionMapper.insert(pi);

        Integer maxSeq = statusHistoryMapper.selectMaxSequence(piId);
        int seq = (maxSeq == null ? 0 : maxSeq) + 1;
        StatusHistory history = StatusHistory.of(
                idGenerator.nextHistoryId(), piId, seq,
                null, "DRAFT", "INSTRUCTION_CREATED", "USER", now);
        statusHistoryMapper.insert(history);

        return pi;
    }

    /**
     * AUTHORIZED 전이: step2 외부검증 통과 후 호출. DRAFT→AUTHORIZED + 상태이력(seq2 AUTH_PASSED).
     * AUTHORIZED = "검증 완료, 진행 확정" — 검증 전 호출 금지.
     */
    @Transactional
    public void authorize(String paymentInstructionId, Integer version) {
        LocalDateTime now = LocalDateTime.now();

        int updated = paymentInstructionMapper.updateStatus(
                paymentInstructionId, "AUTHORIZED", null, null, version);
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "결제지시 상태 갱신 충돌(AUTHORIZED): " + paymentInstructionId);
        }

        Integer maxSeq = statusHistoryMapper.selectMaxSequence(paymentInstructionId);
        int seq = (maxSeq == null ? 0 : maxSeq) + 1;
        StatusHistory history = StatusHistory.of(
                idGenerator.nextHistoryId(), paymentInstructionId, seq,
                "DRAFT", "AUTHORIZED", "AUTH_PASSED", "USER", now);
        statusHistoryMapper.insert(history);
    }

    /** 외부호출 박제: 호출당 독립 짧은 트랜잭션. txStep1/txStep4와 분리 (P-028 외부=트랜잭션밖 유지). */
    @Transactional
    public void recordExternalCall(ExternalCall externalCall) {
        externalCallMapper.insert(externalCall);
    }

    /**
     * PI 수신예금주명 박제: step2 A-2 수신조회 직후 단독 커밋.
     * version 컬럼 갱신 없음 — authorize 낙관락(WHERE version=0) 보호.
     */
    @Transactional
    public void updateReceiverHolderSnap(String paymentInstructionId,
                                         String receiverHolderNameSnap,
                                         LocalDateTime holderInquiryAt) {
        paymentInstructionMapper.updateReceiverHolderSnap(
                paymentInstructionId, receiverHolderNameSnap, holderInquiryAt);
    }

    /**
     * TX-2 (txStep4): 자행이체 확정 한 트랜잭션 (원자성).
     * PROCESSING 전이(seq3) → 분개 2건(차변=대변) → COMPLETED(seq4) → Outbox → 멱등키완료.
     * @param pi authorize까지 끝난 결제지시 (version은 AUTHORIZED 시점)
     * @param withdrawResult B-3 출금 응답 (balanceBefore/After 박제용)
     * @param depositResult B-4 입금 응답
     * @param command 원 명령 (금액/계좌 등)
     * @return PaymentResult (COMPLETED)
     */
    @Transactional
    public PaymentResult txStep4(PaymentInstruction pi, BalanceTxData withdrawResult,
                                 BalanceTxData depositResult, PaymentCommand command,
                                 String senderHolderName, String receiverHolderName) {
        LocalDateTime now = LocalDateTime.now();
        String piId = pi.getPaymentInstructionId();
        String businessDate = now.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        BigDecimal amount = command.transferAmount();

        // 1. AUTHORIZED→PROCESSING (낙관락, pi.getVersion()+1 = authorize 후 버전)
        int updated1 = paymentInstructionMapper.updateStatus(
                piId, "PROCESSING", null, null, pi.getVersion() + 1);
        if (updated1 == 0) {
            throw new OptimisticLockingFailureException(
                    "결제지시 상태 갱신 충돌(PROCESSING): " + piId);
        }
        Integer maxSeq3 = statusHistoryMapper.selectMaxSequence(piId);
        statusHistoryMapper.insert(StatusHistory.of(
                idGenerator.nextHistoryId(), piId, (maxSeq3 == null ? 0 : maxSeq3) + 1,
                "AUTHORIZED", "PROCESSING", "PROCESSING_STARTED", "SYSTEM", now));

        // 2. journal_no 1번 채번 (분개 그룹, 결제 1건당 1개)
        String journalNo = idGenerator.nextJournalNo();

        // 3. 출금 분개 (송신계좌 DEBIT TRANSFER_OUT)
        Ledger out = Ledger.intraTransferOut(
                idGenerator.nextLedgerId(), piId, command.senderAccountId(),
                journalNo, command.senderAccountId(), senderHolderName,
                amount,
                BigDecimal.valueOf(withdrawResult.balanceBefore()),
                BigDecimal.valueOf(withdrawResult.balanceAfter()),
                "KRW", businessDate, businessDate, businessDate,
                now, "자행이체 출금");
        ledgerMapper.insert(out);

        // 4. 입금 분개 (수신계좌 CREDIT TRANSFER_IN, 같은 journalNo)
        Ledger in = Ledger.intraTransferIn(
                idGenerator.nextLedgerId(), piId, command.receiverAccountNo(),
                journalNo, command.receiverAccountNo(), receiverHolderName,
                amount,
                BigDecimal.valueOf(depositResult.balanceBefore()),
                BigDecimal.valueOf(depositResult.balanceAfter()),
                "KRW", businessDate, businessDate, businessDate,
                now, "자행이체 입금");
        ledgerMapper.insert(in);

        // 5. 차변=대변 검증 (P-014)
        BigDecimal debitSum = out.getAmount();
        BigDecimal creditSum = in.getAmount();
        if (debitSum.compareTo(creditSum) != 0) {
            throw new LedgerBalanceMismatchException(
                    "차변≠대변: DEBIT " + debitSum + " ≠ CREDIT " + creditSum + " (PI " + piId + ")");
        }

        // 6. PROCESSING→COMPLETED (낙관락, pi.getVersion()+2)
        int updated2 = paymentInstructionMapper.updateStatus(
                piId, "COMPLETED", now, null, pi.getVersion() + 2);
        if (updated2 == 0) {
            throw new OptimisticLockingFailureException(
                    "결제지시 상태 갱신 충돌(COMPLETED): " + piId);
        }
        Integer maxSeq4 = statusHistoryMapper.selectMaxSequence(piId);
        statusHistoryMapper.insert(StatusHistory.of(
                idGenerator.nextHistoryId(), piId, (maxSeq4 == null ? 0 : maxSeq4) + 1,
                "PROCESSING", "COMPLETED", "PAYMENT_COMPLETED", "SYSTEM", now));

        // 7. Outbox (PAYMENT_COMPLETED, PENDING)
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "paymentInstructionId", piId,
                    "status", "COMPLETED",
                    "amount", amount,
                    "completedAt", now.toString()));
        } catch (JsonProcessingException e) {
            // Map.of 원시값이라 실제 안 터짐. 터지면 시스템 버그 → 롤백
            throw new IllegalStateException("Outbox payload 직렬화 실패: " + piId, e);
        }
        OutboxMessage outbox = OutboxMessage.of(
                idGenerator.nextMessageId(), piId, "PAYMENT_COMPLETED",
                "v1", payload, now);
        outboxMessageMapper.insert(outbox);

        // 8. 멱등키 완료 (스냅샷 = Outbox payload 재활용)
        idempotencyKeyMapper.updateStatus(command.idempotencyKey(), "COMPLETED", payload);

        return new PaymentResult(piId, pi.getTransactionNo(), "COMPLETED", null, now);
    }

    /**
     * TX-FAIL: 비즈니스 검증 실패(PaymentValidationException) 시 DRAFT→FAILED 확정 트랜잭션.
     * CHECK 3개 충족: failure_category SET / completed_at SET / next_retry_at·next_timeout_at=NULL(updateStatus XML).
     * @param pi txStep1 직후 PI (version=0, status=DRAFT)
     * @param failureCategory INSUFFICIENT_BALANCE / HOLDER_MISMATCH 등 실패 원인 enum
     * @param failedEventType 상태이력 검증실패 이벤트 (BALANCE_CHECK_FAILED 등)
     */
    @Transactional
    public PaymentResult txStepFail(PaymentInstruction pi, String failureCategory, String failedEventType) {
        LocalDateTime now = LocalDateTime.now();
        String piId = pi.getPaymentInstructionId();

        // 1. DRAFT→FAILED (낙관락: F1은 authorize 미거쳐 version=0 → FAILED version=1)
        int updated = paymentInstructionMapper.updateStatus(
                piId, "FAILED", now, failureCategory, pi.getVersion());
        if (updated == 0) {
            throw new OptimisticLockingFailureException("결제지시 상태 갱신 충돌(FAILED): " + piId);
        }

        // 2. 상태이력 seq2: 검증실패 이벤트 (상태 DRAFT 유지, 원인 기록)
        Integer maxSeq = statusHistoryMapper.selectMaxSequence(piId);
        int seq2 = (maxSeq == null ? 0 : maxSeq) + 1;
        statusHistoryMapper.insert(StatusHistory.of(
                idGenerator.nextHistoryId(), piId, seq2,
                "DRAFT", "DRAFT", failedEventType, "SYSTEM", now));

        // 3. 상태이력 seq3: PAYMENT_FAILED (DRAFT→FAILED 전이 확정)
        statusHistoryMapper.insert(StatusHistory.of(
                idGenerator.nextHistoryId(), piId, seq2 + 1,
                "DRAFT", "FAILED", "PAYMENT_FAILED", "SYSTEM", now));

        // 4. Outbox (PAYMENT_FAILED, PENDING) — Outbox 워커가 Kafka 발행
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "paymentInstructionId", piId,
                    "status", "FAILED",
                    "failureCategory", failureCategory,
                    "failedAt", now.toString()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 직렬화 실패: " + piId, e);
        }
        outboxMessageMapper.insert(OutboxMessage.of(
                idGenerator.nextMessageId(), piId, "PAYMENT_FAILED",
                "v1", payload, now));

        // 5. 멱등키 FAILED (재시도 시 동일 응답 반환)
        idempotencyKeyMapper.updateStatus(pi.getIdempotencyKey(), "FAILED", payload);

        return new PaymentResult(piId, pi.getTransactionNo(), "FAILED", failureCategory, now);
    }
}
