package com.bank.payment.outbound.feign.mock;

import com.bank.payment.outbound.feign.DepositBalanceClient;
import com.bank.payment.outbound.feign.dto.BalanceInquiryData;
import com.bank.payment.outbound.feign.dto.BalanceTxData;
import com.bank.payment.common.exception.DepositInboundFailureException;
import com.bank.payment.outbound.feign.dto.DepositRequest;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.LimitInquiryData;
import com.bank.payment.outbound.feign.dto.WithdrawCancelData;
import com.bank.payment.outbound.feign.dto.WithdrawRequest;
import org.springframework.context.annotation.Primary;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("mock")
@Primary
@Component
public class DepositBalanceClientMock implements DepositBalanceClient {

    // S1 마스터: 이몽룡 12345678901234 / 성춘향 12345678905678
    private static final String SENDER   = "12345678901234";
    private static final String RECEIVER = "12345678905678";

    // BOK 테스트 송신계좌: 이몽룡 11011100000 (10억 거액이체용)
    private static final String BOK_SENDER = "11011100000";

    // F8 계좌: B-4 입금 시 시스템 장애 시뮬레이션 (race condition)
    private static final String F8_FAIL_RECEIVER = "12345678909999";

    // 케이스 3: 한도초과 트리거 — perTxLimit=100원으로 설정, 1000원 이상 이체 시 LIMIT_EXCEEDED
    private static final String LIMIT_SENDER = "99990000000001";

    @Override
    public DepositResponse<BalanceInquiryData> getBalance(String accountNo) {
        // BOK_SENDER(11011100000) 및 기존 SENDER(12345678901234): 20억 — 10억 BOK + 100만 KFTC 둘 다 통과
        // RECEIVER(성춘향): 100만 유지
        // 그 외: 500만 유지
        long balance;
        if (BOK_SENDER.equals(accountNo) || SENDER.equals(accountNo)) {
            balance = 2000000000L;
        } else if (RECEIVER.equals(accountNo)) {
            balance = 1000000L;
        } else {
            balance = 5000000L;
        }
        BalanceInquiryData data = new BalanceInquiryData(
                accountNo, balance, balance, 0L, "KRW", "2026-05-16T14:30:00Z", 1);
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<LimitInquiryData> getLimit(String accountNo, String date) {
        // 케이스 3: LIMIT_SENDER — perTxLimit/daily/monthly 모두 100원
        //           → 1000원 이상 이체 시 1회 한도 초과 → LIMIT_EXCEEDED
        if (LIMIT_SENDER.equals(accountNo)) {
            LimitInquiryData data = new LimitInquiryData(
                    accountNo, "2026-05-27",
                    100L, 0L, 100L,
                    100L, 0L, 100L,
                    100L, "PERSONAL_RESTRICTED");
            return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
        }
        // perTxLimit 20억: 10억 BOK 단건 통과. 100만 KFTC는 100만 < 20억이라 회귀 없음.
        // dailyLimit/monthlyLimit도 20억: 10억 단건 일·월 한도 안 걸림.
        LimitInquiryData data = new LimitInquiryData(
                accountNo, "2026-05-27",
                2000000000L, 0L, 2000000000L,
                2000000000L, 0L, 2000000000L,
                2000000000L, "PERSONAL_NORMAL");
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public BalanceTxData withdraw(String idempotencyKey, WithdrawRequest request) {
        // before=20억: 10억 출금 후 after=10억(양수), 100만 출금 후 after≈20억(양수).
        // chk_ledger_balance_before/after >= 0 조건 유지.
        BigDecimal before = BigDecimal.valueOf(2000000000L);
        BigDecimal amount = request.amount();
        return new BalanceTxData(
                100045678L,
                "T-20260516-A-00045678", String.valueOf(request.accountId()),
                amount, before, before.subtract(amount),
                "2026-05-16T14:30:00Z", "TRANSFER_OUT");
    }

    @Override
    public BalanceTxData deposit(String idempotencyKey, DepositRequest request) {
        // F8 계좌: 시스템 장애(race condition) 시뮬레이션 → DepositInboundFailureException
        if (DepositAccountClientMock.F8_FAIL_RECEIVER_ID.equals(request.accountId())) {
            throw new DepositInboundFailureException("INTERNAL_SERVER_ERROR", "입금 처리 중 시스템 오류");
        }
        // S1: 성춘향 1,000,000 → (1,000,000 + amount)
        BigDecimal before = BigDecimal.valueOf(1000000L);
        BigDecimal amount = request.amount();
        return new BalanceTxData(
                100067890L,
                "T-20260516-B-00067890", String.valueOf(request.accountId()),
                amount, before, before.add(amount),
                "2026-05-16T14:30:00Z", "TRANSFER_IN");
    }

    // B-5: 출금취소 — 항상 성공. 잔액 복원 시뮬레이션 (20억 기준)
    @Override
    public WithdrawCancelData withdrawCancel(String idempotencyKey, Long transactionId) {
        BigDecimal balanceBefore = BigDecimal.valueOf(1_999_000_000L); // 출금 후 잔액 (100만 출금 기준)
        BigDecimal balanceAfter  = BigDecimal.valueOf(2_000_000_000L); // 취소 후 복원 = 20억
        return new WithdrawCancelData(
                "T-20260516-A-CANCEL-001",
                null,               // originalDepositTransactionNo: deposit 미지원
                null,               // accountNo: deposit 미지원
                balanceBefore,
                balanceAfter,
                "2026-05-16T14:30:00Z");
    }
}
