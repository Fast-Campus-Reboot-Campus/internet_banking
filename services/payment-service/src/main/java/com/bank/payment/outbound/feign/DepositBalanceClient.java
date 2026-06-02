package com.bank.payment.outbound.feign;

import com.bank.payment.outbound.feign.dto.BalanceInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.bank.payment.outbound.feign.dto.DepositRequest;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.LimitInquiryData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelRequest;
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "deposit-balance",
    url = "${deposit.balance.url:http://localhost:8082}",
    configuration = DepositFeignConfig.class,
    primary = false
)
public interface DepositBalanceClient {

    // B-1 잔액조회 — D-REQ-3: deposit에 /balances 전용 endpoint 없음. 해결 전 mock 의존.
    @GetMapping("/api/v1/balances/{accountNo}")
    DepositResponse<BalanceInquiryData> getBalance(@PathVariable("accountNo") String accountNo);

    // B-2 한도조회 — D-REQ-4: deposit에 /limits endpoint 없음. 해결 전 mock 의존.
    @GetMapping("/api/v1/limits/{accountNo}")
    DepositResponse<LimitInquiryData> getLimit(
            @PathVariable("accountNo") String accountNo,
            @RequestParam(value = "date", required = false) String date);

    // B-3 출금 (멱등키 @RequestHeader)
    // deposit 실제 경로: POST /api/transactions/withdraw (TransactionController.java:46)
    // 실패(4xx/5xx) 시 DepositFeignErrorDecoder → DepositInboundFailureException
    @PostMapping("/api/transactions/withdraw")
    BalanceTxData withdraw(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody WithdrawRequest request);

    // B-4 입금 (멱등키 @RequestHeader)
    // deposit 실제 경로: POST /api/transactions/deposit (TransactionController.java:39)
    // 실패(4xx/5xx) 시 DepositFeignErrorDecoder → DepositInboundFailureException
    @PostMapping("/api/transactions/deposit")
    BalanceTxData deposit(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody DepositRequest request);

    // B-5 출금취소 — D-REQ-2: deposit은 PATCH /api/transactions/{transactionId}(Long PK) 기반.
    // payment의 transactionNumber(String)를 transactionId(Long)로 변환하는 endpoint 추가 전 미동작.
    @PostMapping("/api/v1/balances/withdraw/cancel")
    DepositResponse<WithdrawCancelData> withdrawCancel(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody WithdrawCancelRequest request);
}
