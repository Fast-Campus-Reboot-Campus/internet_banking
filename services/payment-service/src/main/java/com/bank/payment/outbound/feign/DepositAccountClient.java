package com.bank.payment.outbound.feign;

import com.bank.payment.outbound.feign.dto.AccountInquiryData;
import com.bank.payment.outbound.feign.dto.DepositResponse;
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
    @GetMapping("/api/v1/accounts/{accountNo}")
    DepositResponse<AccountInquiryData> getAccount(@PathVariable("accountNo") String accountNo);

    // A-2 예금주조회
    @GetMapping("/api/v1/accounts/{accountNo}/holder")
    DepositResponse<HolderInquiryData> getHolder(@PathVariable("accountNo") String accountNo);
}
