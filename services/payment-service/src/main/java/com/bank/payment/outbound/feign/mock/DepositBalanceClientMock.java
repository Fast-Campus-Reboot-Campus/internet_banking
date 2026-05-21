package com.bank.payment.outbound.feign.mock;

import com.bank.payment.outbound.feign.DepositBalanceClient;
import com.bank.payment.outbound.feign.dto.BalanceInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.bank.payment.outbound.feign.dto.DepositRequest;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.LimitInquiryData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelRequest;
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("mock")
@Primary
@Component
public class DepositBalanceClientMock implements DepositBalanceClient {

    // S1 마스터 잔액: 이몽룡 5,000,000 / 성춘향 1,000,000
    private static final String SENDER   = "12345678901234";
    private static final String RECEIVER = "12345678905678";

    // F8 계좌: B-4 입금 시 시스템 장애 시뮬레이션 (race condition)
    private static final String F8_FAIL_RECEIVER = "12345678909999";

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
                30000000L, 0L, 30000000L,
                100000000L, 0L, 100000000L,
                10000000L, "PERSONAL_NORMAL");
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<BalanceTxData> withdraw(String idempotencyKey, WithdrawRequest request) {
        // S1/F8 공통: 이몽룡 5,000,000 → (5,000,000 - amount)
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
        // F8 계좌: 시스템 장애(race condition) 시뮬레이션 → DEP-9001, data=null
        if (F8_FAIL_RECEIVER.equals(request.accountNo())) {
            return new DepositResponse<>("DEP-9001", "INTERNAL_ERROR", "2026-05-16T14:30:00Z", null);
        }
        // S1: 성춘향 1,000,000 → (1,000,000 + amount)
        long before = 1000000L;
        long after = before + request.amount();
        BalanceTxData data = new BalanceTxData(
                "T-20260516-B-00067890", request.accountNo(),
                request.amount(), before, after,
                "2026-05-16T14:30:00Z", "TRANSFER_IN");
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    // B-5: 출금취소 — 항상 성공. 이몽룡 잔액 복원 시뮬레이션
    @Override
    public DepositResponse<WithdrawCancelData> withdrawCancel(String idempotencyKey,
                                                               WithdrawCancelRequest request) {
        long before = 5000000L - request.amount();  // B-3 출금 후 잔액
        long after  = before + request.amount();     // 취소 후 복원 잔액 = 5,000,000
        WithdrawCancelData data = new WithdrawCancelData(
                "T-20260516-A-CANCEL-001", request.originalDepositTransactionNo(),
                request.accountNo(), request.amount(), before, after,
                "2026-05-16T14:30:00Z");
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }
}
