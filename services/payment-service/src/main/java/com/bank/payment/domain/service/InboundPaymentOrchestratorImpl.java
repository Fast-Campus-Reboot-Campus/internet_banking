package com.bank.payment.domain.service;

import com.bank.payment.common.IdGenerator;
import com.bank.payment.domain.ExternalCall;
import com.bank.payment.domain.PaymentInstruction;
import com.bank.payment.outbound.feign.DepositAccountClient;
import com.bank.payment.outbound.feign.DepositBalanceClient;
import com.bank.payment.outbound.feign.dto.AccountInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.bank.payment.outbound.feign.dto.DepositRequest;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * IN 방향 수신 흐름 구현 (정책 시트4).
 * IN-01: 정상 수신 완결. IN-03: 수신계좌 검증 실패 → DRAFT→FAILED + CT REJECTED + KFTC_REJECT_SENT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundPaymentOrchestratorImpl implements InboundPaymentOrchestrator {

    private final PaymentTransactionService txService;
    private final DepositAccountClient depositAccountClient;
    private final DepositBalanceClient depositBalanceClient;
    private final IdGenerator idGenerator;

    @Override
    public void processInbound(String piId, InboundPaymentCommand command) {
        PaymentInstruction pi = txService.selectById(piId);
        String receiverAccountNo = pi.getReceiverAccountNo();

        // A-1: 수신계좌 검증
        DepositResponse<AccountInquiryData> accountResp = depositAccountClient.getAccount(receiverAccountNo);
        recordCall(piId, "ACCOUNT_INQUIRY", "RECEIVER", "deposit", "GET",
                "/api/v1/accounts/" + receiverAccountNo, accountResp.code());
        AccountInquiryData account = accountResp.data();

        if (!"ACTIVE".equals(account.accountStatus()) || Boolean.TRUE.equals(account.fraudFlag())) {
            // mock이 E2001 반환하면 그대로 사용, 아니면 조건에서 매핑 (향후 다른 거절코드 대비)
            String rejectCode = "DEP-0000".equals(accountResp.code()) ? "E2001" : accountResp.code();
            String rejectMsg  = "DEP-0000".equals(accountResp.code())
                    ? "수신계좌 제한: " + account.accountStatus()
                    : accountResp.message();
            txService.txInboundReject(pi, command, rejectCode, rejectMsg);
            log.info("[IN] IN-03 거절 완결: piId={} clearingNo={} rejectCode={}", piId, command.clearingNo(), rejectCode);
            return;
        }

        // TX-IN-AUTH: DRAFT→AUTHORIZED (낙관락 version=0)
        txService.txInboundAuthorize(pi);
        pi = txService.selectById(piId);  // version 갱신 (1)

        // B-4: 입금
        String callIdemKey = piId + "-BALANCE_DEPOSIT-RECEIVER-1";
        DepositRequest depositReq = new DepositRequest(
                receiverAccountNo,
                pi.getTransferAmount().longValueExact(),
                "KRW", "TRANSFER_IN", piId,
                new DepositRequest.Counterparty(
                        command.senderBankCode(),
                        command.senderAccountNo(),
                        command.senderRealName(),
                        pi.getReceiverPassbookSenderDisplay()),
                pi.getReceiverPassbookSenderDisplay());

        DepositResponse<BalanceTxData> depositResp = depositBalanceClient.deposit(callIdemKey, depositReq);
        boolean depositOk = "DEP-0000".equals(depositResp.code());
        recordCall(piId, "BALANCE_DEPOSIT", "RECEIVER", "deposit", "POST",
                "/api/v1/balances/deposit", depositResp.code(), depositOk ? "SUCCESS" : "FAIL");

        if (!depositOk) {
            log.warn("[IN] TODO step③-c: 입금 실패. piId={} code={}", piId, depositResp.code());
            return;
        }

        // TX-IN-DEP: 분개 + CT + COMPLETED + Outbox
        txService.txInboundDeposit(pi, depositResp.data(), command);

        log.info("[IN] IN-01 완결: piId={} clearingNo={}", piId, command.clearingNo());
    }

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
}
