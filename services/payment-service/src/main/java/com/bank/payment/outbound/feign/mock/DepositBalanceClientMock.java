package com.bank.payment.outbound.feign.mock;

import com.bank.payment.outbound.feign.DepositBalanceClient;
import com.bank.payment.outbound.feign.dto.BalanceInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.bank.payment.outbound.feign.dto.DepositRequest;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.LimitInquiryData;
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("mock")
@Component
public class DepositBalanceClientMock implements DepositBalanceClient {

    // S1 마스터 잔액: 이몽룡 5,000,000 / 성춘향 1,000,000
    private static final String SENDER = "12345678901234";
    private static final String RECEIVER = "12345678905678";

    @Override
    public DepositResponse<BalanceInquiryData> getBalance(String accountNo) {
        long balance = RECEIVER.equals(accountNo) ? 1000000L : 5000000L;
        BalanceInquiryData data = new BalanceInquiryData(
                accountNo, balance, balance, 0L, "KRW", "2026-05-16T14:30:00Z", 1);
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<LimitInquiryData> getLimit(String accountNo, String date) {
        LimitInquiryData data = new LimitInquiryData(
                accountNo, "2026-05-16",
                30000000L, 0L, 30000000L,        // daily limit/used/remaining
                100000000L, 0L, 100000000L,      // monthly
                10000000L, "PERSONAL_NORMAL");   // perTx, tier
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<BalanceTxData> withdraw(String idempotencyKey, WithdrawRequest request) {
        // S1: 이몽룡 5,000,000 → 4,500,000 (50만 출금)
        long before = 5000000L;
        long after = before - request.amount();
        BalanceTxData data = new BalanceTxData(
                "T-20260516-A-00045678", request.accountNo(),
                request.amount(), before, after,
                "2026-05-16T14:30:00Z", "TRANSFER_OUT");
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<BalanceTxData> deposit(String idempotencyKey, DepositRequest request) {
        // S1: 성춘향 1,000,000 → 1,500,000 (50만 입금)
        long before = 1000000L;
        long after = before + request.amount();
        BalanceTxData data = new BalanceTxData(
                "T-20260516-B-00067890", request.accountNo(),
                request.amount(), before, after,
                "2026-05-16T14:30:00Z", "TRANSFER_IN");
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }
}
