package com.bank.payment.domain.service;

import com.bank.payment.common.BankCodeMapper;
import com.bank.payment.common.IdGenerator;
import com.bank.payment.common.exception.DepositInboundFailureException;
import com.bank.payment.common.exception.LedgerInsertFailureException;
import com.bank.payment.common.exception.PaymentCancelConflictException;
import com.bank.payment.common.exception.PaymentNotFoundException;
import com.bank.payment.common.exception.PaymentValidationException;
import com.bank.payment.domain.BokSettlementTransaction;
import com.bank.payment.domain.ExternalCall;
import com.bank.payment.domain.KftcClearingTransaction;
import com.bank.payment.domain.Ledger;
import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.outbound.feign.DepositAccountClient;
import com.bank.payment.outbound.feign.DepositBalanceClient;
import com.bank.payment.outbound.feign.dto.AccountInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.bank.payment.outbound.feign.dto.DepositRequest;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.HolderInquiryData;
import com.bank.payment.outbound.feign.dto.LimitInquiryData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelRequest;
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * P-028 5단계 흐름 구현. 외부호출(Feign)은 여기서 트랜잭션 밖. DB 작업은 PaymentTransactionService 위임.
 *
 * Stage 5-6: 자행 S1 8건 (수신검증 추가).
 * F8 (다2): B-4 입금실패 → AUTHORIZED→REVERSING→FAILED + B-5 출금취소. 역분개 0건.
 * F2: KFTC 거절 → CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소.
 * call_idempotency_key 형식: {piId}-{callType}-{accountRole}-{attemptNo}
 */
@Slf4j
@Service
public class PaymentOrchestratorImpl implements PaymentOrchestrator {

    private final PaymentTransactionService txService;
    private final DepositAccountClient depositAccountClient;
    private final DepositBalanceClient depositBalanceClient;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    @Value("${payment.bank-code:A}")
    private String bankCode;

    public PaymentOrchestratorImpl(
            PaymentTransactionService txService,
            DepositAccountClient depositAccountClient,
            DepositBalanceClient depositBalanceClient,
            IdGenerator idGenerator,
            ObjectMapper objectMapper) {
        this.txService = txService;
        this.depositAccountClient = depositAccountClient;
        this.depositBalanceClient = depositBalanceClient;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentResult processPayment(PaymentCommand command) {
        boolean isIntraBank = isIntraBank(command.receiverBankCode());

        // P-007: 10억(1,000,000,000) 이상은 BOK(한은망 거액이체), 미만은 KFTC(금융결제원).
        // ★정책충돌: 테이블정의서 CHECK 예시 1억은 오류로 판단 — enum #16/#39 "10억" 채택.
        String routingNetworkType;
        if (isIntraBank) {
            routingNetworkType = "INTERNAL";
        } else if (command.transferAmount().compareTo(BigDecimal.valueOf(1_000_000_000L)) >= 0) {
            routingNetworkType = "BOK";
        } else {
            routingNetworkType = "KFTC";
        }

        // TX-1: PI DRAFT INSERT — 실패 시 예외가 PaymentValidationException이 아니므로 try 밖
        PaymentInstruction pi = txService.txStep1(command, isIntraBank, routingNetworkType);

        if (isIntraBank) {
            return processIntraBank(pi, command);
        } else if ("BOK".equals(routingNetworkType)) {
            return processInterBok(pi, command);
        } else {
            return processInterBank(pi, command);
        }
    }

    private PaymentResult processIntraBank(PaymentInstruction pi, PaymentCommand command) {
        // B-4 실패 보상 경로에서 B-5 target에 넣을 원 B-3 callId 보관
        WithdrawStepResult withdrawStep = null;

        try {
            ExternalValidationResult validation = step2_externalValidation(pi, command);

            txService.authorize(pi.getPaymentInstructionId(), pi.getVersion());

            // Step 3: 출금(B-3) + 입금(B-4) — 트랜잭션 밖
            withdrawStep = step3_withdraw(pi, command);
            BalanceTxData depositResult = step3b_deposit(pi, command);

            // TX-2: 분개 2건 + COMPLETED + Outbox + 멱등키완료
            return txService.txStep4(pi, withdrawStep.txData(), depositResult, command,
                    validation.senderHolderName(), validation.receiverHolderName());

        } catch (PaymentValidationException e) {
            // 비즈니스 거절 → DRAFT→FAILED. 자금변동 없음(B-3 미도달). 200 OK + status=FAILED
            return txService.txStepFail(pi, e.getFailureCategory(), failedEventTypeFor(e.getFailureCategory()));

        } catch (DepositInboundFailureException e) {
            // B-4 입금 실패: B-3 출금은 성공 → 자금변동 발생 → 보상 필수 (P-002)
            // withdrawStep은 B-3 성공 후 B-4 실패이므로 non-null 보장
            String piId = pi.getPaymentInstructionId();

            // 이중보상 가드: 이미 FAILED/CANCELED이면 skip (합의서 시트15 1차 방어)
            PaymentInstruction freshPi = txService.selectById(piId);
            if ("FAILED".equals(freshPi.getStatus()) || "CANCELED".equals(freshPi.getStatus())) {
                return new PaymentResult(piId, pi.getTransactionNo(), "FAILED", "SYSTEM_ERROR", null);
            }

            // TX-A: AUTHORIZED→REVERSING + 이력 2건
            // pi.getVersion()=0 → authorize 후 DB version=1 → txMarkReversing WHERE version=1 → version=2
            txService.txMarkReversing(pi, pi.getVersion() + 1);

            // B-5: 출금취소 (TX 밖)
            step3c_withdrawCancel(pi, command, withdrawStep.callId(), withdrawStep.txData());

            // TX-B: REVERSING→FAILED + 이력 2건 + Outbox + 멱등키
            // WHERE version=2 → version=3
            return txService.txCompleteReversal(pi, command.idempotencyKey(), pi.getVersion() + 2);

        } catch (LedgerInsertFailureException e) {
            // F5: txStep4 분개 INSERT 실패 → txStep4 전체 롤백 → AUTHORIZED v1 복귀 → 보상 필수 (P-002)
            // B-3 출금·B-4 입금 모두 성공 후 분개만 실패이므로 B-5 출금취소 필수
            // withdrawStep은 B-3/B-4 모두 성공 후 txStep4 실패이므로 non-null 보장
            String piId = pi.getPaymentInstructionId();

            // ★version 신선화: txStep4 롤백으로 DB version=1(AUTHORIZED) 복귀.
            // Java pi.version=0(txStep1 기준)이므로 freshPi로 DB 실제값(1) 확인 후 낙관락 매칭
            PaymentInstruction freshPi = txService.selectById(piId);

            // 이중보상 가드: 이미 FAILED/CANCELED이면 skip (합의서 시트15 1차 방어)
            if ("FAILED".equals(freshPi.getStatus()) || "CANCELED".equals(freshPi.getStatus())) {
                return new PaymentResult(piId, pi.getTransactionNo(), "FAILED", "SYSTEM_ERROR", null);
            }

            // TX-A: AUTHORIZED→REVERSING + 이력 2건 (freshPi.version=1 → WHERE version=1, DB version→2)
            txService.txMarkReversing(freshPi, freshPi.getVersion());

            // B-5: 출금취소 (TX 밖, compensation_target_call_id=원 B-3 callId)
            step3c_withdrawCancel(freshPi, command, withdrawStep.callId(), withdrawStep.txData());

            // TX-B: REVERSING→FAILED + 이력 2건 + Outbox + 멱등키 (freshPi.version+1=2 → WHERE version=2, DB version→3)
            return txService.txCompleteReversal(freshPi, command.idempotencyKey(), freshPi.getVersion() + 1);
        }
    }

    /** BOK 거액이체 송신. step2/authorize/step3는 망 무관 공용 호출. */
    private PaymentResult processInterBok(PaymentInstruction pi, PaymentCommand command) {
        try {
            ExternalValidationResult validation = step2_externalValidation(pi, command);

            txService.authorize(pi.getPaymentInstructionId(), pi.getVersion());

            // Step 3: 출금(B-3) — BOK도 수신 입금 없음, 청산대기 분개(KB-CLR-BOK)로 박제
            WithdrawStepResult withdrawStep = step3_withdraw(pi, command);

            // TX-2: 분개4건(2묶음) + AUTHORIZED→PROCESSING→CLEARING + Outbox(BOK_REQUEST_SENT)
            //       + bok_settlement_transaction REQUESTED INSERT + 멱등키완료
            String numericBankCode = BankCodeMapper.toNumeric(bankCode);
            return txService.txStep4InterBok(pi, withdrawStep.txData(), command,
                    validation.senderHolderName(), numericBankCode);

        } catch (PaymentValidationException e) {
            // step2 검증 실패 — 자금변동 없음(B-3 미도달). 200 OK + status=FAILED
            return txService.txStepFail(pi, e.getFailureCategory(), failedEventTypeFor(e.getFailureCategory()));
        }
    }

    private PaymentResult processInterBank(PaymentInstruction pi, PaymentCommand command) {
        try {
            ExternalValidationResult validation = step2_externalValidation(pi, command);

            txService.authorize(pi.getPaymentInstructionId(), pi.getVersion());

            // Step 3: 출금(B-3) — 타행은 수신 입금 없음, 청산대기 분개로 박제
            WithdrawStepResult withdrawStep = step3_withdraw(pi, command);

            // TX-2: 분개4건(2묶음) + AUTHORIZED→PROCESSING→CLEARING + Outbox(KFTC_REQUEST_SENT)
            //       + kftc_clearing_transaction REQUESTED INSERT + 멱등키완료
            String numericBankCode = BankCodeMapper.toNumeric(bankCode);
            return txService.txStep4InterBank(pi, withdrawStep.txData(), command,
                    validation.senderHolderName(), numericBankCode);

        } catch (PaymentValidationException e) {
            // step2 검증 실패 — 자금변동 없음(B-3 미도달). 200 OK + status=FAILED
            return txService.txStepFail(pi, e.getFailureCategory(), failedEventTypeFor(e.getFailureCategory()));
        }
    }

    private static String failedEventTypeFor(String failureCategory) {
        return switch (failureCategory) {
            case "INSUFFICIENT_BALANCE" -> "BALANCE_CHECK_FAILED";
            default -> "VALIDATION_FAILED";
        };
    }

    // receiverBankCode == 자행코드(A은행=004, B은행=088) → 자행
    private boolean isIntraBank(String receiverBankCode) {
        return BankCodeMapper.toNumeric(bankCode).equals(receiverBankCode);
    }

    /**
     * Step 2: 외부검증 8건 (합의서 시트17 S1 순서).
     * A-1송신 → A-1수신 → A-2송신 → A-2수신(HOLDER_DECEASED/HOLDER_MISMATCH) → B-1 → B-2
     * 모두 트랜잭션 밖. PI receiver_holder_name_snap은 A-2수신 직후 단독 커밋.
     */
    private ExternalValidationResult step2_externalValidation(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        String sender = command.senderAccountId();
        String receiver = command.receiverAccountNo();

        // A-1 계좌조회 (송신계좌)
        DepositResponse<AccountInquiryData> senderAccountResp = depositAccountClient.getAccount(sender);
        recordCall(piId, "ACCOUNT_INQUIRY", "SENDER", "deposit", "GET",
                "/api/v1/accounts/" + sender, senderAccountResp.code());
        AccountInquiryData senderAccount = senderAccountResp.data();
        if (!"ACTIVE".equals(senderAccount.accountStatus())) {
            throw new PaymentValidationException("ACCOUNT_INACTIVE",
                    "송신계좌 비활성: " + senderAccount.accountStatus());
        }
        if (Boolean.TRUE.equals(senderAccount.fraudFlag())) {
            throw new PaymentValidationException("FRAUD_REPORTED", "송신계좌 사고신고");
        }

        // A-1 계좌조회 (수신계좌) — 자행만. 타행은 수신계좌가 타 은행 관할이므로 deposit 검증 생략
        if (Boolean.TRUE.equals(pi.getIsIntraBank())) {
            DepositResponse<AccountInquiryData> receiverAccountResp = depositAccountClient.getAccount(receiver);
            recordCall(piId, "ACCOUNT_INQUIRY", "RECEIVER", "deposit", "GET",
                    "/api/v1/accounts/" + receiver, receiverAccountResp.code());
            AccountInquiryData receiverAccount = receiverAccountResp.data();
            if (!"ACTIVE".equals(receiverAccount.accountStatus())) {
                throw new PaymentValidationException("ACCOUNT_INACTIVE",
                        "수신계좌 비활성: " + receiverAccount.accountStatus());
            }
            if (Boolean.TRUE.equals(receiverAccount.fraudFlag())) {
                throw new PaymentValidationException("FRAUD_REPORTED", "수신계좌 사고신고");
            }
        }

        // A-2 예금주조회 (송신계좌)
        DepositResponse<HolderInquiryData> senderHolderResp = depositAccountClient.getHolder(sender);
        recordCall(piId, "ACCOUNT_OWNER_INQUIRY", "SENDER", "deposit", "GET",
                "/api/v1/accounts/" + sender + "/holder", senderHolderResp.code());
        String senderHolderName = senderHolderResp.data().holderName();

        // A-2 예금주조회 (수신계좌) — 자행만. 타행은 요청값 그대로 박제 (KFTC가 수신측 검증)
        String receiverHolderName;
        if (Boolean.TRUE.equals(pi.getIsIntraBank())) {
            LocalDateTime receiverHolderInquiryAt = LocalDateTime.now();
            DepositResponse<HolderInquiryData> receiverHolderResp = depositAccountClient.getHolder(receiver);
            recordCall(piId, "ACCOUNT_OWNER_INQUIRY", "RECEIVER", "deposit", "GET",
                    "/api/v1/accounts/" + receiver + "/holder", receiverHolderResp.code());
            HolderInquiryData receiverHolder = receiverHolderResp.data();
            if (Boolean.TRUE.equals(receiverHolder.deceasedFlag())) {
                throw new PaymentValidationException("HOLDER_DECEASED", "수신 예금주 사망");
            }
            if (!receiverHolder.holderName().equals(command.receiverHolderName())) {
                throw new PaymentValidationException("HOLDER_MISMATCH",
                        "수신자명 불일치: 입력=" + command.receiverHolderName()
                        + ", 조회=" + receiverHolder.holderName());
            }
            receiverHolderName = receiverHolder.holderName();
            txService.updateReceiverHolderSnap(piId, receiverHolderName, receiverHolderInquiryAt);
        } else {
            // 타행: 수신 예금주명은 요청값 그대로 박제 (holderInquiryAt=null, V8 nullable)
            receiverHolderName = command.receiverHolderName();
            txService.updateReceiverHolderSnap(piId, receiverHolderName, null);
        }

        // B-1 잔액조회 (송신계좌) — 결과 확인 후 박제 (FAIL/SUCCESS 분기)
        DepositResponse<BalanceInquiryData> balanceResp = depositBalanceClient.getBalance(sender);
        BalanceInquiryData balance = balanceResp.data();
        long needed = command.transferAmount().longValueExact();
        if (balance.availableBalance() < needed) {
            recordCall(piId, "BALANCE_INQUIRY", "SENDER", "deposit", "GET",
                    "/api/v1/balances/" + sender, balanceResp.code(), "FAIL");
            throw new PaymentValidationException("INSUFFICIENT_BALANCE",
                    "잔액 부족: 가용 " + balance.availableBalance() + " < 필요 " + needed);
        }
        recordCall(piId, "BALANCE_INQUIRY", "SENDER", "deposit", "GET",
                "/api/v1/balances/" + sender, balanceResp.code());

        // B-2 한도조회 (송신계좌)
        DepositResponse<LimitInquiryData> limitResp = depositBalanceClient.getLimit(sender, null);
        recordCall(piId, "LIMIT_CHECK", "SENDER", "deposit", "GET",
                "/api/v1/limits/" + sender, limitResp.code());
        LimitInquiryData limit = limitResp.data();
        if (needed > limit.perTxLimit()) {
            throw new PaymentValidationException("SINGLE_TX_LIMIT", "1회 한도 초과");
        }
        if (needed > limit.dailyRemaining()) {
            throw new PaymentValidationException("DAILY_LIMIT_EXCEEDED", "일일 한도 초과");
        }
        if (needed > limit.monthlyRemaining()) {
            throw new PaymentValidationException("MONTHLY_LIMIT_EXCEEDED", "월 한도 초과");
        }

        return new ExternalValidationResult(senderHolderName, receiverHolderName);
    }

    // ── Step 3: 출금 (B-3, 트랜잭션 밖) ─────────────────────────────────────
    // WithdrawStepResult: BalanceTxData + callId (B-4 실패 시 B-5 compensation_target_call_id 참조용)
    private WithdrawStepResult step3_withdraw(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        long amount = command.transferAmount().longValueExact();
        String callIdemKey = piId + "-BALANCE_WITHDRAW-SENDER-1";

        WithdrawRequest request = new WithdrawRequest(
                command.senderAccountId(), amount, "KRW", "TRANSFER_OUT", piId,
                new WithdrawRequest.Counterparty(
                        command.receiverBankCode(), command.receiverAccountNo(), command.receiverHolderName()),
                command.senderMemo());

        DepositResponse<BalanceTxData> resp = depositBalanceClient.withdraw(callIdemKey, request);
        String callId = recordCall(piId, "BALANCE_WITHDRAW", "SENDER", "deposit", "POST",
                "/api/v1/balances/withdraw", resp.code());
        return new WithdrawStepResult(resp.data(), callId);
    }

    // ── Step 3b: 입금 (B-4, 트랜잭션 밖, 자행 수신) ──────────────────────────
    // DEP-0000 외 응답 코드 → DepositInboundFailureException (보상 필요 신호, P-002)
    private BalanceTxData step3b_deposit(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        long amount = command.transferAmount().longValueExact();
        String callIdemKey = piId + "-BALANCE_DEPOSIT-RECEIVER-1";

        DepositRequest request = new DepositRequest(
                command.receiverAccountNo(), amount, "KRW", "TRANSFER_IN", piId,
                new DepositRequest.Counterparty(
                        command.receiverBankCode(), command.senderAccountId(), command.receiverHolderName(),
                        command.receiverPassbookSenderDisplay()),
                command.receiverMemo());

        DepositResponse<BalanceTxData> resp = depositBalanceClient.deposit(callIdemKey, request);
        boolean success = "DEP-0000".equals(resp.code());
        recordCall(piId, "BALANCE_DEPOSIT", "RECEIVER", "deposit", "POST",
                "/api/v1/balances/deposit", resp.code(), success ? "SUCCESS" : "FAIL");
        if (!success) {
            throw new DepositInboundFailureException(resp.code(),
                    "B-4 입금 실패: " + resp.code() + " / " + resp.message());
        }
        return resp.data();
    }

    // ── Step 3c: 출금취소 (B-5, 트랜잭션 밖, F8 보상 전용) ──────────────────
    // compensation_type=COMPENSATION, compensation_target_call_id=원 B-3 callId
    private void step3c_withdrawCancel(PaymentInstruction pi, PaymentCommand command,
                                        String originalWithdrawCallId, BalanceTxData withdrawTxData) {
        String piId = pi.getPaymentInstructionId();
        long amount = command.transferAmount().longValueExact();
        String callIdemKey = piId + "-BALANCE_WITHDRAW_CANCEL-SENDER-1";

        WithdrawCancelRequest request = new WithdrawCancelRequest(
                withdrawTxData.depositTransactionNo(),  // 원 B-3 deposit common_transaction no
                command.senderAccountId(),
                amount,
                "PAYMENT_FAILED",
                piId);

        var resp = depositBalanceClient.withdrawCancel(callIdemKey, request);
        recordCall(piId, "BALANCE_WITHDRAW_CANCEL", "SENDER", "deposit", "POST",
                "/api/v1/balances/withdraw/cancel", resp.code(), "SUCCESS",
                originalWithdrawCallId);  // ← compensation_target_call_id = 원 B-3 callId
    }

    // ── F2: KFTC 거절 보상 ────────────────────────────────────────────────────

    /**
     * F2 KFTC 거절 보상. CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + CT REJECTED.
     * 결정 (f) 재진입 가드: FAILED→skip / CLEARING|REVERSING→진행 / 그 외→warn+skip.
     */
    @Override
    public PaymentResult processKftcReject(
            PaymentInstruction freshPi, String clearingNo,
            String rejectCode, String rejectMessage, String rejectedAt) {

        String piId = freshPi.getPaymentInstructionId();
        String status = freshPi.getStatus();

        // 결정 (f) 멱등 가드
        if ("FAILED".equals(status)) {
            log.info("[F2] 이미 FAILED, skip. piId={}", piId);
            return new PaymentResult(piId, freshPi.getTransactionNo(), "FAILED", "KFTC_REJECTED", null);
        }
        if (!"CLEARING".equals(status) && !"REVERSING".equals(status)) {
            log.warn("[F2] 처리불가 상태, skip. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }

        boolean wasClearing = "CLEARING".equals(status);

        // TX-1: CLEARING → REVERSING (CLEARING 진입 시에만)
        if (wasClearing) {
            txService.txMarkReversingFromClearing(freshPi, freshPi.getVersion(), rejectMessage, "KFTC", "E2001");
        }

        // TX-2 낙관락 버전: TX-1 후 DB version+1 / REVERSING 재진입은 현재 version 그대로
        Integer tx2Version = wasClearing ? freshPi.getVersion() + 1 : freshPi.getVersion();

        // B-5: 출금취소 (TX 밖) — REVERSING 재진입 시 이미 수행된 경우 skip
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[F2] B-5 이미 수행됨, skip 재호출. piId={} existingCallId={}",
                    piId, existingCancelCall.getCallId());
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelForReject(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건 + FAILED + CT REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteKftcRejectReversal(
                freshPi, tx2Version, originals, cancelResult, rejectCode, rejectMessage, clearingNo,
                "KFTC_REJECTION", "KFTC_REJECTED", "EXTERNAL_REJECTION");
    }

    // ── F3: BOK 거절 보상 ────────────────────────────────────────────────────

    /**
     * F3 BOK 거절 보상. CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + BST REJECTED.
     * processKftcReject의 BOK판. 재진입 가드 동일: FAILED→skip / CLEARING|REVERSING→진행 / 그 외→warn+skip.
     */
    @Override
    public PaymentResult processBokReject(
            PaymentInstruction freshPi, String bokReferenceNo,
            String rejectCode, String rejectMessage, String rejectedAt) {

        String piId = freshPi.getPaymentInstructionId();
        String status = freshPi.getStatus();

        // 멱등 가드 (⑥)
        if ("FAILED".equals(status)) {
            log.info("[F3] 이미 FAILED, skip. piId={}", piId);
            return new PaymentResult(piId, freshPi.getTransactionNo(), "FAILED", "BOK_REJECTED", null);
        }
        if (!"CLEARING".equals(status) && !"REVERSING".equals(status)) {
            log.warn("[F3] 처리불가 상태, skip. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }

        boolean wasClearing = "CLEARING".equals(status);

        // TX-1: CLEARING → REVERSING (CLEARING 진입 시에만)
        if (wasClearing) {
            txService.txMarkReversingFromClearing(freshPi, freshPi.getVersion(), rejectMessage, "BOK", rejectCode);
        }

        // TX-2 낙관락 버전: TX-1 후 DB version+1 / REVERSING 재진입은 현재 version 그대로
        Integer tx2Version = wasClearing ? freshPi.getVersion() + 1 : freshPi.getVersion();

        // B-5: 출금취소 (TX 밖) — REVERSING 재진입 시 이미 수행된 경우 skip
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[F3] B-5 이미 수행됨, skip 재호출. piId={} existingCallId={}",
                    piId, existingCancelCall.getCallId());
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelForReject(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건 + FAILED + BST REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteBokRejectReversal(
                freshPi, tx2Version, originals, cancelResult, rejectCode, rejectMessage, bokReferenceNo);
    }

    // ── F4: KFTC 송신실패 자동보상 ──────────────────────────────────────────────

    /**
     * F4 KFTC 송신실패 보상. OutboxPublisher가 KFTC_REQUEST_SENT send 실패 시 호출.
     * F2형 보상 재사용: CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + CT REJECTED.
     * reversal_reason=PUBLISH_FAILURE / PI.failure_category=SYSTEM_ERROR.
     */
    @Override
    public PaymentResult processPublishFailure(String piId, String lastError) {
        PaymentInstruction freshPi = txService.selectById(piId);
        if (freshPi == null) {
            log.error("[F4] PI 조회 실패, 보상 불가. piId={}", piId);
            return null;
        }

        String status = freshPi.getStatus();

        // 이중보상 가드
        if ("FAILED".equals(status) || "CANCELED".equals(status)) {
            log.info("[F4] 이중보상 가드: 이미 처리됨. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if ("REVERSING".equals(status)) {
            log.info("[F4] 이중보상 가드: 보상 진행중. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if (!"CLEARING".equals(status)) {
            log.warn("[F4] 예상치 못한 상태, skip. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }

        String rejectMsg = "KFTC 송신 실패: " + (lastError != null ? lastError : "");
        if (rejectMsg.length() > 200) {
            rejectMsg = rejectMsg.substring(0, 197) + "...";
        }

        // TX-1: CLEARING→CLEARING(KFTC_REQUEST_FAILED) + CLEARING→REVERSING(REVERSAL_STARTED)
        txService.txMarkReversingFromClearing(freshPi, freshPi.getVersion(), rejectMsg, "SYSTEM", "PUBLISH_FAILURE", "KFTC_REQUEST_FAILED");
        Integer tx2Version = freshPi.getVersion() + 1;

        // B-5: 출금취소 (TX 밖)
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall   = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[F4] B-5 이미 수행됨, skip 재호출. piId={}", piId);
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo    = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelForReject(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건(PUBLISH_FAILURE) + FAILED/SYSTEM_ERROR + CT REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteKftcRejectReversal(
                freshPi, tx2Version, originals, cancelResult,
                "PUBLISH_FAILURE", rejectMsg, null,
                "PUBLISH_FAILURE", "SYSTEM_ERROR", "PUBLISH_FAILURE");
    }

    // ── F4 BOK: 송신실패 자동보상 ────────────────────────────────────────────────

    /**
     * F4 BOK 송신실패 보상. OutboxPublisher가 BOK_REQUEST_SENT send 실패 시 호출.
     * F3형 보상 재사용: CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + BST REJECTED.
     * reversal_reason=PUBLISH_FAILURE / PI.failure_category=SYSTEM_ERROR.
     */
    @Override
    public PaymentResult processBokPublishFailure(String piId, String lastError) {
        PaymentInstruction freshPi = txService.selectById(piId);
        if (freshPi == null) {
            log.error("[BOK F4] PI 조회 실패, 보상 불가. piId={}", piId);
            return null;
        }

        String status = freshPi.getStatus();

        // 이중보상 가드 (KFTC F4와 동일)
        if ("FAILED".equals(status) || "CANCELED".equals(status)) {
            log.info("[BOK F4] 이중보상 가드: 이미 처리됨. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if ("REVERSING".equals(status)) {
            log.info("[BOK F4] 이중보상 가드: 보상 진행중. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if (!"CLEARING".equals(status)) {
            log.warn("[BOK F4] 예상치 못한 상태, skip. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }

        String rejectMsg = "BOK 송신 실패: " + (lastError != null ? lastError : "");
        if (rejectMsg.length() > 200) {
            rejectMsg = rejectMsg.substring(0, 197) + "...";
        }

        // TX-1: CLEARING→CLEARING(BOK_REQUEST_FAILED) + CLEARING→REVERSING(REVERSAL_STARTED)
        txService.txMarkReversingFromClearing(freshPi, freshPi.getVersion(), rejectMsg, "SYSTEM", "PUBLISH_FAILURE", "BOK_REQUEST_FAILED");
        Integer tx2Version = freshPi.getVersion() + 1;

        // B-5: 출금취소 (TX 밖)
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall   = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[BOK F4] B-5 이미 수행됨, skip 재호출. piId={}", piId);
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo    = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelForReject(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건(PUBLISH_FAILURE) + FAILED/SYSTEM_ERROR + BST REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        // updateRejected는 piId WHERE — bokReferenceNo 불필요(null 전달)
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteBokRejectReversal(
                freshPi, tx2Version, originals, cancelResult,
                "PUBLISH_FAILURE", rejectMsg, null,
                "PUBLISH_FAILURE", "SYSTEM_ERROR", "PUBLISH_FAILURE",
                "SYSTEM", null);
    }

    // ── F7: KFTC 정산실패 자동보상 ──────────────────────────────────────────────

    /**
     * F7 KFTC 정산실패 보상. SETTLEMENT_NOTIFY responseCode != "0000" 수신 시 호출.
     * F4형 보상 재사용: CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + CT REJECTED.
     * reversal_reason=SETTLEMENT_FAILURE / PI.failure_category=SYSTEM_ERROR.
     */
    @Override
    public PaymentResult processSettlementFailure(String clearingNo, String responseCode, String rejectMessage) {
        KftcClearingTransaction ct = txService.selectByClearingNo(clearingNo);
        if (ct == null) {
            log.warn("[F7] CT 조회 실패, 보상 불가. clearingNo={}", clearingNo);
            return null;
        }
        String piId = ct.getOurPaymentInstructionId();
        PaymentInstruction freshPi = txService.selectById(piId);
        if (freshPi == null) {
            log.error("[F7] PI 조회 실패, 보상 불가. piId={}", piId);
            return null;
        }

        String status = freshPi.getStatus();

        // 이중보상 가드
        if ("FAILED".equals(status) || "CANCELED".equals(status)) {
            log.info("[F7] 이중보상 가드: 이미 처리됨. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if ("REVERSING".equals(status)) {
            log.info("[F7] 이중보상 가드: 보상 진행중. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        // 정상완결 후 뒤늦은 실패통보 — 정책 시트6 케이스3 "범위 외/운영자" (P-014)
        if ("COMPLETED".equals(status)) {
            log.warn("[F7] PI 이미 COMPLETED(범위 외), 보상 skip. piId={}", piId);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if (!"CLEARING".equals(status)) {
            log.warn("[F7] 예상치 못한 상태, skip. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }

        String rejectMsg = "KFTC 정산실패: " + (rejectMessage != null && !rejectMessage.isEmpty() ? rejectMessage : responseCode);
        if (rejectMsg.length() > 200) {
            rejectMsg = rejectMsg.substring(0, 197) + "...";
        }

        // TX-1: CLEARING→CLEARING(KFTC_SETTLEMENT_FAILED) + CLEARING→REVERSING(REVERSAL_STARTED)
        txService.txMarkReversingFromClearing(freshPi, freshPi.getVersion(), rejectMsg, "SYSTEM", "SETTLEMENT_FAILURE", "KFTC_SETTLEMENT_FAILED");
        Integer tx2Version = freshPi.getVersion() + 1;

        // B-5: 출금취소 (TX 밖)
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall   = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[F7] B-5 이미 수행됨, skip 재호출. piId={}", piId);
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo    = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelForReject(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건(SETTLEMENT_FAILURE) + FAILED/SYSTEM_ERROR + CT REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteKftcRejectReversal(
                freshPi, tx2Version, originals, cancelResult,
                "SETTLEMENT_FAILURE", rejectMsg, clearingNo,
                "SETTLEMENT_FAILURE", "SYSTEM_ERROR", "SETTLEMENT_FAILURE");
    }

    // ── F7 BOK: 정산실패 자동보상 ────────────────────────────────────────────────

    /**
     * F7 BOK 정산실패 보상. SETTLEMENT_COMPLETED responseCode != "0000" 수신 시 호출.
     * F3형 보상 재사용: CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + BST REJECTED.
     * reversal_reason=SETTLEMENT_FAILURE / PI.failure_category=SYSTEM_ERROR.
     */
    @Override
    public PaymentResult processBokSettlementFailure(String bokReferenceNo, String responseCode, String rejectMessage) {
        BokSettlementTransaction bst = txService.selectByBokReferenceNo(bokReferenceNo);
        if (bst == null) {
            log.warn("[BOK-F7] BST 조회 실패, 보상 불가. bokReferenceNo={}", bokReferenceNo);
            return null;
        }
        String piId = bst.getOurPaymentInstructionId();
        PaymentInstruction freshPi = txService.selectById(piId);
        if (freshPi == null) {
            log.error("[BOK-F7] PI 조회 실패, 보상 불가. piId={}", piId);
            return null;
        }

        String status = freshPi.getStatus();

        if ("FAILED".equals(status) || "CANCELED".equals(status)) {
            log.info("[BOK-F7] 이중보상 가드: 이미 처리됨. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if ("REVERSING".equals(status)) {
            log.info("[BOK-F7] 이중보상 가드: 보상 진행중. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if ("COMPLETED".equals(status)) {
            log.warn("[BOK-F7] PI 이미 COMPLETED(범위 외), 보상 skip. piId={}", piId);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }
        if (!"CLEARING".equals(status)) {
            log.warn("[BOK-F7] 예상치 못한 상태, skip. piId={} status={}", piId, status);
            return new PaymentResult(piId, freshPi.getTransactionNo(), status, null, null);
        }

        String rejectMsg = "BOK 정산실패: " + (rejectMessage != null && !rejectMessage.isEmpty() ? rejectMessage : responseCode);
        if (rejectMsg.length() > 200) {
            rejectMsg = rejectMsg.substring(0, 197) + "...";
        }

        // TX-1: CLEARING→CLEARING(BOK_SETTLEMENT_FAILED) + CLEARING→REVERSING(REVERSAL_STARTED)
        txService.txMarkReversingFromClearing(freshPi, freshPi.getVersion(), rejectMsg, "SYSTEM", "SETTLEMENT_FAILURE", "BOK_SETTLEMENT_FAILED");
        Integer tx2Version = freshPi.getVersion() + 1;

        // B-5: 출금취소 (TX 밖)
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall   = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[BOK-F7] B-5 이미 수행됨, skip 재호출. piId={}", piId);
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo    = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelForReject(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건(SETTLEMENT_FAILURE) + FAILED/SYSTEM_ERROR + BST REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteBokRejectReversal(
                freshPi, tx2Version, originals, cancelResult,
                "SETTLEMENT_FAILURE",   // rejectCode
                rejectMsg,              // rejectMessage
                bokReferenceNo,         // bokReferenceNo (BST updateRejected WHERE piId 기준, 참조용)
                "SETTLEMENT_FAILURE",   // reversalReason
                "SYSTEM_ERROR",         // failureCategory
                "SETTLEMENT_FAILURE",   // outboxFailureCategory
                "SYSTEM",               // triggeredBy
                null);                  // operatorId
    }

    // ── F6-Ⅱ-2: 운영자 강제취소 ──────────────────────────────────────────────────

    /**
     * 운영자 강제취소. CLEARING 상태만 허용. CLEARING→REVERSING→FAILED + 역분개4건 + B-5 + CT REJECTED.
     * reversal_reason=OPERATOR / failure_category=SYSTEM_ERROR / triggered_by=OPERATOR / operator_id 박제.
     */
    @Override
    public PaymentResult processOperatorCancel(String piId, String operatorId, String reason) {
        PaymentInstruction freshPi = txService.selectById(piId);
        if (freshPi == null) {
            throw new PaymentNotFoundException(piId);
        }

        String status = freshPi.getStatus();
        if (!"CLEARING".equals(status)) {
            throw new PaymentCancelConflictException(status);
        }

        // TX-1: CLEARING→CLEARING(OPERATOR_CANCEL_DECIDED, OPERATOR, operatorId)
        //       + CLEARING→REVERSING(REVERSAL_STARTED, OPERATOR, operatorId)
        txService.txMarkReversingFromClearing(
                freshPi, freshPi.getVersion(),
                reason, "OPERATOR", "OPERATOR", "OPERATOR_CANCEL_DECIDED", operatorId);
        Integer tx2Version = freshPi.getVersion() + 1;

        // B-5: 출금취소 (TX 밖, reason=OPERATOR_CANCEL)
        ExternalCall originalWithdrawCall = txService.selectOriginalWithdrawCall(piId);
        ExternalCall existingCancelCall   = txService.selectExistingCancelCall(piId);

        WithdrawCancelData cancelResult = null;
        if (existingCancelCall != null) {
            log.info("[F6] B-5 이미 수행됨, skip 재호출. piId={}", piId);
        } else {
            String originalCallId = (originalWithdrawCall != null) ? originalWithdrawCall.getCallId() : null;
            String depositTxNo    = extractDepositTxNo(originalWithdrawCall);
            cancelResult = performWithdrawCancelOperator(piId, freshPi, originalCallId, depositTxNo);
        }

        // TX-2: 역분개4건(OPERATOR) + FAILED/SYSTEM_ERROR + CT REJECTED + Outbox PAYMENT_REVERSED + 멱등키
        List<Ledger> originals = txService.selectOriginalsByPaymentId(piId);
        return txService.txCompleteKftcRejectReversal(
                freshPi, tx2Version, originals, cancelResult,
                "OPERATOR",     // rejectCode (CT reject_code 및 status_history reason_code)
                reason,         // rejectMessage (운영자 사유)
                null,           // clearingNo (CT는 piId로 조회하므로 불사용)
                "OPERATOR",     // reversalReason (역분개4 reversal_reason)
                "SYSTEM_ERROR", // failureCategory (PI.failure_category)
                "OPERATOR",     // outboxFailureCategory (Outbox payload)
                "OPERATOR",     // triggeredBy (REVERSING→FAILED 이력 triggered_by)
                operatorId);    // operatorId
    }

    /**
     * B-5 출금취소 호출 (운영자 취소 전용, reason=OPERATOR_CANCEL).
     * performWithdrawCancelForReject와 동일 패턴, reason만 다름.
     */
    private WithdrawCancelData performWithdrawCancelOperator(
            String piId, PaymentInstruction pi, String originalCallId, String depositTxNo) {
        String callIdemKey = piId + "-BALANCE_WITHDRAW_CANCEL-SENDER-1";
        long amount = pi.getTransferAmount().longValueExact();

        WithdrawCancelRequest request = new WithdrawCancelRequest(
                depositTxNo,
                pi.getSenderAccountId(),
                amount,
                "OPERATOR_CANCEL",
                piId);

        DepositResponse<WithdrawCancelData> resp = depositBalanceClient.withdrawCancel(callIdemKey, request);
        recordCall(piId, "BALANCE_WITHDRAW_CANCEL", "SENDER", "deposit", "POST",
                "/api/v1/balances/withdraw/cancel", resp.code(), "SUCCESS",
                originalCallId);
        return resp.data();
    }

    /**
     * B-5 응답 JSON에서 depositTransactionNo 추출.
     * mock에서는 responseBody가 "{}"이므로 "" fallback.
     */
    private String extractDepositTxNo(ExternalCall call) {
        if (call == null) return "";
        try {
            JsonNode body = objectMapper.readTree(call.getResponseBody());
            return body.path("depositTransactionNo").asText("");
        } catch (Exception e) {
            log.warn("[F2] depositTransactionNo 파싱 실패, 빈값 사용. callId={}", call.getCallId());
            return "";
        }
    }

    /**
     * B-5 출금취소 호출 + external_call 박제 (F2 보상 전용).
     * compensation_type=COMPENSATION, compensation_target_call_id=원 B-3 callId.
     */
    private WithdrawCancelData performWithdrawCancelForReject(
            String piId, PaymentInstruction pi, String originalCallId, String depositTxNo) {
        String callIdemKey = piId + "-BALANCE_WITHDRAW_CANCEL-SENDER-1";
        long amount = pi.getTransferAmount().longValueExact();

        WithdrawCancelRequest request = new WithdrawCancelRequest(
                depositTxNo,
                pi.getSenderAccountId(),
                amount,
                "PAYMENT_FAILED",
                piId);

        DepositResponse<WithdrawCancelData> resp = depositBalanceClient.withdrawCancel(callIdemKey, request);
        recordCall(piId, "BALANCE_WITHDRAW_CANCEL", "SENDER", "deposit", "POST",
                "/api/v1/balances/withdraw/cancel", resp.code(), "SUCCESS",
                originalCallId);
        return resp.data();
    }

    // ── recordCall 오버로드 ───────────────────────────────────────────────────

    /**
     * 외부호출 박제 (ORIGINAL). callId 반환 — B-3 callId를 B-5 target에 넣기 위해 필요.
     * call_idempotency_key 형식: {piId}-{callType}-{accountRole}-1
     * result: SUCCESS(기본) 또는 FAIL(B-1 잔액부족 등 비즈니스 거절).
     */
    private String recordCall(String piId, String callType, String accountRole,
                              String targetSystem, String httpMethod, String endpointUrl,
                              String responseCode) {
        return recordCall(piId, callType, accountRole, targetSystem, httpMethod, endpointUrl,
                responseCode, "SUCCESS");
    }

    private String recordCall(String piId, String callType, String accountRole,
                              String targetSystem, String httpMethod, String endpointUrl,
                              String responseCode, String result) {
        LocalDateTime now = LocalDateTime.now();
        String callId = idGenerator.nextCallId();
        String callIdemKey = piId + "-" + callType + "-" + accountRole + "-1";
        ExternalCall ec = ExternalCall.of(
                callId, callIdemKey, piId,
                callType, targetSystem, endpointUrl, httpMethod,
                UUID.randomUUID().toString(), "{}", "{}", "",
                500, now);
        ec.recordResponse(200, "{}", "{}", responseCode, result, result, 50, now);
        txService.recordExternalCall(ec);
        return callId;
    }

    /** 보상 외부호출 박제. compensation_type=COMPENSATION + compensationTargetCallId 필수 (V4 CHECK). */
    private String recordCall(String piId, String callType, String accountRole,
                              String targetSystem, String httpMethod, String endpointUrl,
                              String responseCode, String result, String compensationTargetCallId) {
        LocalDateTime now = LocalDateTime.now();
        String callId = idGenerator.nextCallId();
        String callIdemKey = piId + "-" + callType + "-" + accountRole + "-1";
        ExternalCall ec = ExternalCall.ofCompensation(
                callId, callIdemKey, piId, compensationTargetCallId,
                callType, targetSystem, endpointUrl, httpMethod,
                UUID.randomUUID().toString(), "{}", "{}", "",
                500, now);
        ec.recordResponse(200, "{}", "{}", responseCode, result, result, 50, now);
        txService.recordExternalCall(ec);
        return callId;
    }
}
