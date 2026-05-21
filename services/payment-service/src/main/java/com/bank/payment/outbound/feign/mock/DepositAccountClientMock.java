package com.bank.payment.outbound.feign.mock;

import com.bank.payment.outbound.feign.DepositAccountClient;
import com.bank.payment.outbound.feign.dto.AccountInquiryData;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.HolderInquiryData;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("mock")
@Primary
@Component
public class DepositAccountClientMock implements DepositAccountClient {

    // S1 마스터: 이몽룡 12345678901234 / 성춘향 12345678905678
    private static final String SENDER = "12345678901234";
    private static final String RECEIVER = "12345678905678";

    @Override
    public DepositResponse<AccountInquiryData> getAccount(String accountNo) {
        AccountInquiryData data = new AccountInquiryData(
                accountNo, "DEMAND", "ACTIVE", "DP-2025-001",
                "2024-03-15T09:00:00Z", null, "0001", false, 1);
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<HolderInquiryData> getHolder(String accountNo) {
        String holder = RECEIVER.equals(accountNo) ? "성춘향" : "이몽룡";
        HolderInquiryData data = new HolderInquiryData(
                accountNo, holder, "INDIVIDUAL", "CUST-0001", false, 1);
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }
}
