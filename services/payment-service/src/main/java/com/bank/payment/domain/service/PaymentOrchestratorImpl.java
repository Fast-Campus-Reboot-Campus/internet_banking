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
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import com.bank.payment.outbound.feign.dto.HolderInquiryData;
import com.bank.payment.outbound.feign.dto.LimitInquiryData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P-028 5단계 흐름 구현. 외부호출(Feign)은 여기서 트랜잭션 밖. DB 작업은 PaymentTransactionService 위임.
 *
 * Stage 5-3: txStep1 → step2 외부검증 → authorize.
 * Stage 5-4: step3 출금/입금 + txStep4 (분개/COMPLETED/Outbox) — TODO.
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

        PaymentInstruction pi = txService.txStep1(command, isIntraBank, routingNetworkType);

        String senderHolderName = step2_externalValidation(pi, command);

        txService.authorize(pi.getPaymentInstructionId(), pi.getVersion());

        // Step 3: 출금(B-3) + 입금(B-4) — 트랜잭션 밖
        BalanceTxData withdrawResult = step3_withdraw(pi, command);
        BalanceTxData depositResult = step3b_deposit(pi, command);

        // TX-2: 분개 2건 + COMPLETED + Outbox + 멱등키완료
        return txService.txStep4(pi, withdrawResult, depositResult, command, senderHolderName);
    }

    // receiverBankCode == 자행코드(A은행=004, B은행=088) → 자행
    private boolean isIntraBank(String receiverBankCode) {
        String myBankCode = "B".equalsIgnoreCase(bankCode) ? "088" : "004";
        return myBankCode.equals(receiverBankCode);
    }

    private String step2_externalValidation(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        String sender = command.senderAccountId();

        // A-1 계좌조회 (송신계좌)
        DepositResponse<AccountInquiryData> accountResp = depositAccountClient.getAccount(sender);
        recordCall(piId, "ACCOUNT_INQUIRY", "deposit", "GET",
                "/api/v1/accounts/" + sender, accountResp.code());
        AccountInquiryData account = accountResp.data();
        if (!"ACTIVE".equals(account.accountStatus())) {
            throw new PaymentValidationException("ACCOUNT_INACTIVE",
                    "송신계좌 비활성: " + account.accountStatus());
        }
        if (Boolean.TRUE.equals(account.fraudFlag())) {
            throw new PaymentValidationException("FRAUD_REPORTED", "송신계좌 사고신고");
        }

        // A-1.5 송신예금주조회 (분개 holder_name_snap 박제용)
        DepositResponse<HolderInquiryData> senderHolderResp = depositAccountClient.getHolder(sender);
        recordCall(piId, "ACCOUNT_OWNER_INQUIRY", "deposit", "GET",
                "/api/v1/accounts/" + sender + "/holder", senderHolderResp.code());
        String senderHolderName = senderHolderResp.data().holderName();

        // B-1 잔액조회 (송신계좌)
        DepositResponse<BalanceInquiryData> balanceResp = depositBalanceClient.getBalance(sender);
        recordCall(piId, "BALANCE_INQUIRY", "deposit", "GET",
                "/api/v1/balances/" + sender, balanceResp.code());
        BalanceInquiryData balance = balanceResp.data();
        long needed = command.transferAmount().longValueExact();
        if (balance.availableBalance() < needed) {
            throw new PaymentValidationException("INSUFFICIENT_BALANCE",
                    "잔액 부족: 가용 " + balance.availableBalance() + " < 필요 " + needed);
        }

        // B-2 한도조회 (송신계좌)
        DepositResponse<LimitInquiryData> limitResp = depositBalanceClient.getLimit(sender, null);
        recordCall(piId, "LIMIT_CHECK", "deposit", "GET",
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

        return senderHolderName;
    }

    // S1 단순화: 요청+응답 한 번에 insert. 타임아웃/실패 추적 필요 시 insert→update 분리 (TODO).
    private void recordCall(String piId, String callType, String targetSystem,
                            String httpMethod, String endpointUrl, String responseCode) {
        LocalDateTime now = LocalDateTime.now();
        String callId = idGenerator.nextCallId();
        ExternalCall ec = ExternalCall.of(
                callId,
                callType + "-" + piId + "-001",    // callIdempotencyKey: {호출종류}-{PI}-{시도번호}
                piId,
                callType, targetSystem, endpointUrl, httpMethod,
                UUID.randomUUID().toString(),       // requestId: 호출별 고유 UUID
                "{}", "{}", "",
                500, now);
        ec.recordResponse(200, "{}", "{}", responseCode, "SUCCESS", "SUCCESS", 50, now);
        txService.recordExternalCall(ec);
    }

    // ── Step 3: 출금 (B-3, 트랜잭션 밖) ─────────────────
    private BalanceTxData step3_withdraw(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        long amount = command.transferAmount().longValueExact();

        WithdrawRequest request = new WithdrawRequest(
                command.senderAccountId(), amount, "KRW", "TRANSFER_OUT", piId,
                new WithdrawRequest.Counterparty(
                        command.receiverBankCode(), command.receiverAccountNo(), command.receiverHolderName()),
                command.senderMemo());
        String callIdemKey = "BALANCE_WITHDRAW-" + piId + "-001";

        DepositResponse<BalanceTxData> resp = depositBalanceClient.withdraw(callIdemKey, request);
        recordCall(piId, "BALANCE_WITHDRAW", "deposit", "POST",
                "/api/v1/balances/withdraw", resp.code());
        return resp.data();
    }

    // ── Step 3b: 입금 (B-4, 트랜잭션 밖, 자행 수신) ──────
    // ★ 보상 공백: 출금 성공 후 입금 실패 시 출금만 됨 → 보상(REVERSAL_TRANSFER_OUT) 필요.
    //   S1 mock 항상 성공이라 안 탐. 보상 흐름은 P-026/F8 (Stage 7). 현재 예외 전파.
    private BalanceTxData step3b_deposit(PaymentInstruction pi, PaymentCommand command) {
        String piId = pi.getPaymentInstructionId();
        long amount = command.transferAmount().longValueExact();

        DepositRequest request = new DepositRequest(
                command.receiverAccountNo(), amount, "KRW", "TRANSFER_IN", piId,
                new DepositRequest.Counterparty(
                        command.receiverBankCode(), command.senderAccountId(), command.receiverHolderName(),
                        command.receiverPassbookSenderDisplay()),
                command.receiverMemo());
        String callIdemKey = "BALANCE_DEPOSIT-" + piId + "-001";

        DepositResponse<BalanceTxData> resp = depositBalanceClient.deposit(callIdemKey, request);
        recordCall(piId, "BALANCE_DEPOSIT", "deposit", "POST",
                "/api/v1/balances/deposit", resp.code());
        return resp.data();
    }
}
