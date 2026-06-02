package com.bank.payment.outbound.feign.mock;

import com.bank.payment.outbound.feign.DepositAccountClient;
import com.bank.payment.outbound.feign.dto.AccountInquiryData;
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

    // 테스트 전용 mock accountId (deposit PK 대용)
    static final Long SENDER_ID           = 1L;
    static final Long RECEIVER_ID         = 2L;
    static final Long F5_FAIL_RECEIVER_ID = 3L;
    static final Long IN03_FROZEN_ID      = 4L;
    static final Long CLOSED_RECEIVER_ID  = 5L;
    static final Long FRAUD_RECEIVER_ID   = 6L;
    static final Long HOLDER_MISMATCH_ID  = 7L;
    static final Long F8_FAIL_RECEIVER_ID = 8L;
    static final Long DEFAULT_ID          = 99L;

    // 테스트 전용: 동적 CLOSED 상태 (closeAccount/openAccount/resetAllClosed 으로 제어)
    private final Set<String> closedAccounts = Collections.synchronizedSet(new HashSet<>());

    public void closeAccount(String accountNo) { closedAccounts.add(accountNo); }
    public void openAccount(String accountNo)  { closedAccounts.remove(accountNo); }
    public void resetAllClosed()               { closedAccounts.clear(); }

    // A-1: 계좌번호 기반 조회 (D-REQ-1 해결 — by-number 엔드포인트 사용)
    @Override
    public AccountInquiryData getAccountByNo(String accountNo) {
        Long accountId = resolveAccountId(accountNo);
        if (closedAccounts.contains(accountNo)) {
            return new AccountInquiryData(accountId, accountNo, "DEMAND", "CLOSED", "DP-2025-001",
                    "2024-03-15T09:00:00Z", "2026-01-01T00:00:00Z", "0001", false, 1);
        }
        if (IN03_FROZEN_RECEIVER.equals(accountNo)) {
            // IN-03: accountStatus≠ACTIVE → E2001 거절 트리거
            return new AccountInquiryData(accountId, accountNo, "DEMAND", "SUSPENDED", "DP-2025-001",
                    "2024-03-15T09:00:00Z", null, "0001", true, 1);
        }
        if (CLOSED_RECEIVER.equals(accountNo)) {
            return new AccountInquiryData(accountId, accountNo, "DEMAND", "CLOSED", "DP-2025-001",
                    "2024-03-15T09:00:00Z", "2026-01-01T00:00:00Z", "0001", false, 1);
        }
        if (FRAUD_RECEIVER.equals(accountNo)) {
            return new AccountInquiryData(accountId, accountNo, "DEMAND", "ACTIVE", "DP-2025-001",
                    "2024-03-15T09:00:00Z", null, "0001", true, 1);
        }
        return new AccountInquiryData(accountId, accountNo, "DEMAND", "ACTIVE", "DP-2025-001",
                "2024-03-15T09:00:00Z", null, "0001", false, 1);
    }

    // A-1: accountId(Long PK) 기반 직접 조회 — mock에서는 getAccountByNo 위임
    @Override
    public AccountInquiryData getAccount(String accountId) {
        return getAccountByNo(accountId);
    }

    // A-2 예금주조회 — D-REQ-5: deposit 미제공. mock 하네스 유지.
    @Override
    public HolderInquiryData getHolder(String accountNo) {
        if (HOLDER_MISMATCH_RECEIVER.equals(accountNo)) {
            // 케이스 4: 예금주명 "홍길동" 반환 — 요청값(성춘향)과 불일치 → OWNER_INQUIRY_FAILED
            return new HolderInquiryData(accountNo, "홍길동", "INDIVIDUAL", "CUST-0002", false, 1);
        }
        String holder;
        if (F8_FAIL_RECEIVER.equals(accountNo)) {
            holder = "홍판서";
        } else if (F5_FAIL_RECEIVER.equals(accountNo)) {
            holder = "변학도";
        } else if (RECEIVER.equals(accountNo)) {
            holder = "성춘향";
        } else {
            holder = "이몽룡";
        }
        return new HolderInquiryData(accountNo, holder, "INDIVIDUAL", "CUST-0001", false, 1);
    }

    private Long resolveAccountId(String accountNo) {
        return switch (accountNo) {
            case SENDER              -> SENDER_ID;
            case RECEIVER            -> RECEIVER_ID;
            case F5_FAIL_RECEIVER    -> F5_FAIL_RECEIVER_ID;
            case IN03_FROZEN_RECEIVER -> IN03_FROZEN_ID;
            case CLOSED_RECEIVER     -> CLOSED_RECEIVER_ID;
            case FRAUD_RECEIVER      -> FRAUD_RECEIVER_ID;
            case HOLDER_MISMATCH_RECEIVER -> HOLDER_MISMATCH_ID;
            case F8_FAIL_RECEIVER    -> F8_FAIL_RECEIVER_ID;
            default                  -> DEFAULT_ID;
        };
    }
}
