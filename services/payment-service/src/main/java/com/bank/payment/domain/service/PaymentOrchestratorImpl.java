package com.bank.payment.domain.service;

import com.bank.payment.common.IdGenerator;
import com.bank.payment.common.exception.PaymentValidationException;
import com.bank.payment.domain.ExternalCall;
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
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P-028 5단계 흐름 구현. 외부호출(Feign)은 여기서 트랜잭션 밖. DB 작업은 PaymentTransactionService 위임.
 *
 * Stage 5-6: 자행 S1 8건 (수신검증 추가).
 * call_idempotency_key 형식: {piId}-{callType}-{accountRole}-{attemptNo}
 */
@Service
public class PaymentOrchestratorImpl implements PaymentOrchestrator {

    private final PaymentTransactionService txService;
    private final DepositAccountClient depositAccountClient;
    private final DepositBalanceClient depositBalanceClient;
    private final IdGenerator idGenerator;

    @Value("${payment.bank-code:A}")
    private String bankCode;

    public PaymentOrchestratorImpl(
            PaymentTransactionService txService,
            DepositAccountClient depositAccountClient,
            DepositBalanceClient depositBalanceClient,
            IdGenerator idGenerator) {
        this.txService = txService;
        this.depositAccountClient = depositAccountClient;
        this.depositBalanceClient = depositBalanceClient;
        this.idGenerator = idGenerator;
    }

    @Override
    public PaymentResult processPayment(PaymentCommand command) {
        boolean isIntraBank = isIntraBank(command.receiverBankCode());
        String routingNetworkType = isIntraBank ? "INTERNAL" : "EXTERNAL";

        // TX-1: PI DRAFT INSERT — 실패 시 예외가 PaymentValidationException이 아니므로 try 밖
        PaymentInstruction pi = txService.txStep1(command, isIntraBank, routingNetworkType);

        try {
            ExternalValidationResult validation = step2_externalValidation(pi, command);

            txService.authorize(pi.getPaymentInstructionId(), pi.getVersion());

            // Step 3: 출금(B-3) + 입금(B-4) — 트랜잭션 밖
            BalanceTxData withdrawResult = step3_withdraw(pi, command);
            BalanceTxData depositResult = step3b_deposit(pi, command);

            // TX-2: 분개 2건 + COMPLETED + Outbox + 멱등키완료
            return txService.txStep4(pi, withdrawResult, depositResult, command,
                    validation.senderHolderName(), validation.receiverHolderName());
        } catch (PaymentValidationException e) {
            // 비즈니스 거절 → DRAFT→FAILED. 200 OK + status=FAILED 반환 (잔액부족 등은 정상 비즈니스 결과)
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
        String myBankCode = "B".equalsIgnoreCase(bankCode) ? "088" : "004";
        return myBankCode.equals(receiverBankCode);
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

        // A-1 계좌조회 (수신계좌)
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

        // A-2 예금주조회 (송신계좌)
        DepositResponse<HolderInquiryData> senderHolderResp = depositAccountClient.getHolder(sender);
        recordCall(piId, "ACCOUNT_OWNER_INQUIRY", "SENDER", "deposit", "GET",
                "/api/v1/accounts/" + sender + "/holder", senderHolderResp.code());
        String senderHolderName = senderHolderResp.data().holderName();

        // A-2 예금주조회 (수신계좌) — HOLDER_DECEASED + HOLDER_MISMATCH 검증
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
        String receiverHolderName = receiverHolder.holderName();

        // PI receiver_holder_name_snap 박제 — version 변경 없음 (authorize 낙관락 보호)
        txService.updateReceiverHolderSnap(piId, receiverHolderName, receiverHolderInquiryAt);

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

    /**
     * 외부호출 박제. call_idempotency_key 형식: {piId}-{callType}-{accountRole}-1
     * accountRole: SENDER / RECEIVER. 동일 callType 2회 호출(A-1/A-2) UNIQUE 충돌 방지.
     * B-1~B-4도 일관 적용 (1회 호출이라 충돌 없으나 형식 통일).
     * result: "SUCCESS"(기본) 또는 "FAIL"(B-1 잔액부족 등 비즈니스 거절).
     */
    private void recordCall(String piId, String callType, String accountRole,
                            String targetSystem, String httpMethod, String endpointUrl, String responseCode) {
        recordCall(piId, callType, accountRole, targetSystem, httpMethod, endpointUrl, responseCode, "SUCCESS");
    }

    private void recordCall(String piId, String callType, String accountRole,
                            String targetSystem, String httpMethod, String endpointUrl,
                            String responseCode, String result) {
        LocalDateTime now = LocalDateTime.now();
        String callId = idGenerator.nextCallId();
        String callIdemKey = piId + "-" + callType + "-" + accountRole + "-1";
        ExternalCall ec = ExternalCall.of(
                callId,
                callIdemKey,
                piId,
                callType, targetSystem, endpointUrl, httpMethod,
                UUID.randomUUID().toString(),       // requestId: 호출별 고유 UUID
                "{}", "{}", "",
                500, now);
        ec.recordResponse(200, "{}", "{}", responseCode, result, result, 50, now);
        txService.recordExternalCall(ec);
    }

    // ── Step 3: 출금 (B-3, 트랜잭션 밖) ─────────────────
    private BalanceTxData step3_withdraw(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        long amount = command.transferAmount().longValueExact();
        String callIdemKey = piId + "-BALANCE_WITHDRAW-SENDER-1";

        WithdrawRequest request = new WithdrawRequest(
                command.senderAccountId(), amount, "KRW", "TRANSFER_OUT", piId,
                new WithdrawRequest.Counterparty(
                        command.receiverBankCode(), command.receiverAccountNo(), command.receiverHolderName()),
                command.senderMemo());

        DepositResponse<BalanceTxData> resp = depositBalanceClient.withdraw(callIdemKey, request);
        recordCall(piId, "BALANCE_WITHDRAW", "SENDER", "deposit", "POST",
                "/api/v1/balances/withdraw", resp.code());
        return resp.data();
    }

    // ── Step 3b: 입금 (B-4, 트랜잭션 밖, 자행 수신) ──────
    // ★ 보상 공백: 출금 성공 후 입금 실패 시 출금만 됨 → 보상(REVERSAL_TRANSFER_OUT) 필요.
    //   S1 mock 항상 성공이라 안 탐. 보상 흐름은 P-026/F8 (Stage 7). 현재 예외 전파.
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
        recordCall(piId, "BALANCE_DEPOSIT", "RECEIVER", "deposit", "POST",
                "/api/v1/balances/deposit", resp.code());
        return resp.data();
    }
}
