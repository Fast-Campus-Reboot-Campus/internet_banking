package com.bank.payment.domain.service;

import com.bank.payment.common.IdGenerator;
import com.bank.payment.domain.ExternalCall;
import com.bank.payment.domain.IdempotencyKey;
import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.domain.StatusHistory;
import com.bank.payment.domain.mapper.ExternalCallMapper;
import com.bank.payment.domain.mapper.IdempotencyKeyMapper;
import com.bank.payment.domain.mapper.PaymentInstructionMapper;
import com.bank.payment.domain.mapper.StatusHistoryMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public PaymentTransactionService(
            PaymentInstructionMapper paymentInstructionMapper,
            IdempotencyKeyMapper idempotencyKeyMapper,
            StatusHistoryMapper statusHistoryMapper,
            ExternalCallMapper externalCallMapper,
            IdGenerator idGenerator) {
        this.paymentInstructionMapper = paymentInstructionMapper;
        this.idempotencyKeyMapper = idempotencyKeyMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.externalCallMapper = externalCallMapper;
        this.idGenerator = idGenerator;
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
                .receiverHolderNameSnap(command.receiverHolderName()) // 입력값 임시 박제 (step2 A-2 응답으로 확정)
                .holderInquiryAt(now)
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
}
