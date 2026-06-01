package com.bank.payment.outbound.feign.mock;

import com.bank.payment.outbound.feign.DepositAccountClient;
import com.bank.payment.outbound.feign.dto.AccountInquiryData;
import com.bank.payment.outbound.feign.dto.DepositResponse;
import com.bank.payment.outbound.feign.dto.HolderInquiryData;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Profile("mock")
@Primary
@Component
public class DepositAccountClientMock implements DepositAccountClient {

    // S1 마스터: 이몽룡 12345678901234 / 성춘향 12345678905678
    private static final String SENDER           = "12345678901234";
    private static final String RECEIVER         = "12345678905678";

    // F8 계좌: 홍판서 12345678909999 (B-4 입금 실패 트리거용)
    private static final String F8_FAIL_RECEIVER = "12345678909999";

    // F5 계좌: 변학도 88880000 (txStep4 분개 INSERT 실패 트리거용 — deposit까지 성공, 분개에서만 실패)
    private static final String F5_FAIL_RECEIVER = "88880000";

    // IN-03 계좌: 사고신고/FROZEN (수신 거절 트리거용)
    private static final String IN03_FROZEN_RECEIVER = "99987654321";

    // 케이스 5: 수신계좌 폐쇄 트리거 — accountStatus=CLOSED → ACCOUNT_CLOSED
    private static final String CLOSED_RECEIVER = "99990000000003";

    // 케이스 6: 수신계좌 사고신고 트리거 — accountStatus=ACTIVE + fraudFlag=true → ACCOUNT_RESTRICTED
    private static final String FRAUD_RECEIVER = "99990000000004";

    // 케이스 4: 수신계좌 예금주 불일치 트리거 — getHolder에서 "홍길동" 반환
    private static final String HOLDER_MISMATCH_RECEIVER = "99990000000002";

    // 테스트 전용: 동적 CLOSED 상태 (closeAccount/openAccount/resetAllClosed 으로 제어)
    private final Set<String> closedAccounts = Collections.synchronizedSet(new HashSet<>());

    public void closeAccount(String accountNo) { closedAccounts.add(accountNo); }
    public void openAccount(String accountNo)  { closedAccounts.remove(accountNo); }
    public void resetAllClosed()               { closedAccounts.clear(); }

    @Override
    public DepositResponse<AccountInquiryData> getAccount(String accountNo) {
        // 동적 CLOSED 우선 처리 (테스트에서 closeAccount 호출 시 CLOSED 반환)
        if (closedAccounts.contains(accountNo)) {
            AccountInquiryData data = new AccountInquiryData(
                    accountNo, "DEMAND", "CLOSED", "DP-2025-001",
                    "2024-03-15T09:00:00Z", "2026-01-01T00:00:00Z", "0001", false, 1);
            return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
        }
        if (IN03_FROZEN_RECEIVER.equals(accountNo)) {
            AccountInquiryData data = new AccountInquiryData(
                    accountNo, "DEMAND", "FROZEN", "DP-2025-001",
                    "2024-03-15T09:00:00Z", null, "0001", true, 1);
            return new DepositResponse<>("E2001", "사고신고 계좌", "2026-05-16T14:30:00Z", data);
        }
        // 케이스 5: CLOSED 수신계좌 → ACCOUNT_CLOSED
        if (CLOSED_RECEIVER.equals(accountNo)) {
            AccountInquiryData data = new AccountInquiryData(
                    accountNo, "DEMAND", "CLOSED", "DP-2025-001",
                    "2024-03-15T09:00:00Z", "2026-01-01T00:00:00Z", "0001", false, 1);
            return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
        }
        // 케이스 6: ACTIVE + fraudFlag=true → ACCOUNT_RESTRICTED
        if (FRAUD_RECEIVER.equals(accountNo)) {
            AccountInquiryData data = new AccountInquiryData(
                    accountNo, "DEMAND", "ACTIVE", "DP-2025-001",
                    "2024-03-15T09:00:00Z", null, "0001", true, 1);
            return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
        }
        AccountInquiryData data = new AccountInquiryData(
                accountNo, "DEMAND", "ACTIVE", "DP-2025-001",
                "2024-03-15T09:00:00Z", null, "0001", false, 1);
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }

    @Override
    public DepositResponse<HolderInquiryData> getHolder(String accountNo) {
        String holder;
        if (F8_FAIL_RECEIVER.equals(accountNo)) {
            holder = "홍판서";
        } else if (F5_FAIL_RECEIVER.equals(accountNo)) {
            holder = "변학도";
        } else if (RECEIVER.equals(accountNo)) {
            holder = "성춘향";
        } else if (HOLDER_MISMATCH_RECEIVER.equals(accountNo)) {
            // 케이스 4: 예금주명 "홍길동" 반환 — 요청값(성춘향)과 불일치 → OWNER_INQUIRY_FAILED
            HolderInquiryData data = new HolderInquiryData(
                    accountNo, "홍길동", "INDIVIDUAL", "CUST-0002", false, 1);
            return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
        } else {
            holder = "이몽룡";
        }
        HolderInquiryData data = new HolderInquiryData(
                accountNo, holder, "INDIVIDUAL", "CUST-0001", false, 1);
        return new DepositResponse<>("DEP-0000", "SUCCESS", "2026-05-16T14:30:00Z", data);
    }
}
