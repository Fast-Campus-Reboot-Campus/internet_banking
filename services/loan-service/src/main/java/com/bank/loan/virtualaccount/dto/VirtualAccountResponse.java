package com.bank.loan.virtualaccount.dto;

import com.bank.commonaccount.domain.CommonAccount;

/**
 * 대출 상환용 가상계좌 발급 응답.
 */
public record VirtualAccountResponse(
        String accountNo,
        String bankCode,
        String accountNickname
) {
    public static VirtualAccountResponse of(CommonAccount account) {
        return new VirtualAccountResponse(
                account.getAccountNo(),
                account.getBankCd(),
                account.getAccountNickname());
    }
}
