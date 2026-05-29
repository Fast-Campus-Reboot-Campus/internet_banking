package com.bank.loan.payment;

import com.bank.commonaccount.domain.CommonAccount;
import com.bank.commonaccount.repository.CommonAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 여신 시스템 계좌(수납/집행) 조회.
 *
 * 공통 계좌 마스터(common_db.common_account)에서 account_nickname 으로 역할별 계좌를 해석한다.
 * payment 호출 시 여기서 얻은 account_no / bank_cd 를 그대로 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class SystemAccountProvider {

    /** 수납계좌(고객→은행 입금 수취) 식별 닉네임 */
    public static final String NICKNAME_COLLECTION = "LOAN_COLLECTION";
    /** 집행계좌(대출실행 자금 출금) 식별 닉네임 */
    public static final String NICKNAME_DISBURSEMENT = "LOAN_DISBURSEMENT";

    private final CommonAccountRepository commonAccountRepository;

    /** 수납계좌 — 자동이체/온라인/역분개 환급의 은행 측 계좌 */
    public CommonAccount collectionAccount() {
        return require(NICKNAME_COLLECTION);
    }

    /** 집행계좌 — 대출실행 자금 출금 계좌 */
    public CommonAccount disbursementAccount() {
        return require(NICKNAME_DISBURSEMENT);
    }

    private CommonAccount require(String nickname) {
        return commonAccountRepository.findByAccountNickname(nickname)
                .orElseThrow(() -> new IllegalStateException(
                        "common_account 시스템 계좌 미존재: nickname=" + nickname
                                + " (db/common-migration seed 확인)"));
    }
}
