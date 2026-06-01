package com.bank.loan.virtualaccount.service;

import com.bank.common.web.BusinessException;
import com.bank.commonaccount.domain.CommonAccount;
import com.bank.commonaccount.repository.CommonAccountRepository;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 대출 상환용 가상계좌 발급.
 *
 * 중도·추가 상환을 입금(push) 방식으로 받기 위해, 대출별 가상계좌를 공통 계좌 마스터
 * (common_db.common_account, account_type_cd=VIRTUAL)로 발급한다.
 * common_account.contract_id 에 대출 계약을 박아, 추후 입금통지 수신 시 계좌번호→대출 매핑에 사용한다.
 *
 * ⚠️ 미완성(의도적 부분구현): 발급은 동작하지만, 입금 인식(고객 입금 → 상환 완결)은
 *    payment 의 입금통지 이벤트에 수신계좌번호(receiverAccountNo)가 실려야 가능하다.
 *    현재 payment payload 에 해당 필드가 없어 소비 측은 stub 상태다
 *    ({@code com.bank.loan.virtualaccount.kafka.VirtualAccountDepositConsumer} 참고).
 */
@Service
@RequiredArgsConstructor
public class VirtualAccountService {

    private static final String BANK_CD = "004";        // 자행
    private static final String VIRTUAL_PREFIX = "044"; // 가상계좌 식별 프리픽스
    private static final String CURRENCY = "KRW";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CommonAccountRepository commonAccountRepository;
    private final LoanContractRepository contractRepository;

    /**
     * 계약별 가상계좌 발급(멱등). 이미 있으면 기존 계좌를 반환한다.
     */
    @Transactional("commonTransactionManager")
    public CommonAccount issue(Long cntrId) {
        String accountNo = virtualAccountNo(cntrId);
        return commonAccountRepository.findByAccountNo(accountNo)
                .orElseGet(() -> create(cntrId, accountNo));
    }

    private CommonAccount create(Long cntrId, String accountNo) {
        LoanContract contract = contractRepository.findById(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062, "cntrId=" + cntrId));

        CommonAccount va = CommonAccount.builder()
                .accountNo(accountNo)
                .customerId(contract.getCustomerId())
                .contractId(cntrId)
                .accountTypeCd(CommonAccount.TYPE_VIRTUAL)
                .accountNickname("VIRTUAL-" + cntrId)
                .bankCd(BANK_CD)
                .balance(0L)
                .currencyCd(CURRENCY)
                .accountStatus(STATUS_ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        return commonAccountRepository.save(va);
    }

    /** 계약ID 기반 결정적 가상계좌번호: 044 + 10자리 zero-pad cntrId */
    private String virtualAccountNo(Long cntrId) {
        return VIRTUAL_PREFIX + String.format("%010d", cntrId);
    }
}
