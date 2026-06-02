package com.bank.payment.outbound.feign;

import com.bank.payment.outbound.feign.dto.AccountInquiryData;
import com.bank.payment.outbound.feign.dto.HolderInquiryData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "deposit-account",
    url = "${deposit.account.url:http://localhost:8082}",
    configuration = DepositFeignConfig.class,
    primary = false
)
public interface DepositAccountClient {

    // A-1 계좌조회
    // deposit 실제 경로: GET /api/accounts/{accountId}(Long PK) (AccountController.java:36)
    // 실패(4xx/5xx) 시 DepositFeignErrorDecoder → DepositInboundFailureException
    // D-REQ-1: accountNo(String) → accountId(Long) 변환은 deposit GET /api/accounts/by-number/{accountNo} 추가 후 처리.
    @GetMapping("/api/accounts/{accountId}")
    AccountInquiryData getAccount(@PathVariable("accountId") String accountNo);

    // A-2 예금주조회 — D-REQ-5: deposit에 holderName 없음(customerId만 저장). 해결 전 mock 의존.
    @GetMapping("/api/v1/accounts/{accountNo}/holder")
    HolderInquiryData getHolder(@PathVariable("accountNo") String accountNo);
}
