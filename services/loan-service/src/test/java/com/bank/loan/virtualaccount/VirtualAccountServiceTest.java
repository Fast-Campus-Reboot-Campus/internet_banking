package com.bank.loan.virtualaccount;

import com.bank.common.web.BusinessException;
import com.bank.commonaccount.domain.CommonAccount;
import com.bank.commonaccount.repository.CommonAccountRepository;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.support.LoanErrorCode;
import com.bank.loan.virtualaccount.service.VirtualAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualAccountServiceTest {

    @Mock CommonAccountRepository commonAccountRepository;
    @Mock LoanContractRepository contractRepository;
    @InjectMocks VirtualAccountService service;

    private LoanContract contract(Long cntrId, Long customerId) {
        return LoanContract.builder()
                .cntrId(cntrId)
                .customerId(customerId)
                .build();
    }

    private CommonAccount stubAccount(Long cntrId) {
        return CommonAccount.builder()
                .accountNo("044" + String.format("%010d", cntrId))
                .customerId(1001L)
                .contractId(cntrId)
                .accountTypeCd(CommonAccount.TYPE_VIRTUAL)
                .accountNickname("VIRTUAL-" + cntrId)
                .bankCd("004")
                .balance(0L)
                .currencyCd("KRW")
                .accountStatus("ACTIVE")
                .build();
    }

    @Test
    void 신규발급_계약존재_가상계좌없음() {
        Long cntrId = 5L;
        String expectedNo = "0440000000005";

        when(commonAccountRepository.findByAccountNo(expectedNo)).thenReturn(Optional.empty());
        when(contractRepository.findById(cntrId)).thenReturn(Optional.of(contract(cntrId, 1001L)));
        when(commonAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommonAccount result = service.issue(cntrId);

        ArgumentCaptor<CommonAccount> cap = ArgumentCaptor.forClass(CommonAccount.class);
        verify(commonAccountRepository).save(cap.capture());

        CommonAccount saved = cap.getValue();
        assertThat(saved.getAccountNo()).isEqualTo(expectedNo);
        assertThat(saved.getAccountTypeCd()).isEqualTo(CommonAccount.TYPE_VIRTUAL);
        assertThat(saved.getCustomerId()).isEqualTo(1001L);
        assertThat(saved.getContractId()).isEqualTo(cntrId);
        assertThat(saved.getBankCd()).isEqualTo("004");
        assertThat(saved.getBalance()).isEqualTo(0L);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void 멱등_이미발급된_계좌반환() {
        Long cntrId = 7L;
        CommonAccount existing = stubAccount(cntrId);
        when(commonAccountRepository.findByAccountNo("0440000000007")).thenReturn(Optional.of(existing));

        CommonAccount result = service.issue(cntrId);

        assertThat(result).isSameAs(existing);
        verify(contractRepository, never()).findById(any());
        verify(commonAccountRepository, never()).save(any());
    }

    @Test
    void 계약없음_LOAN_062() {
        Long cntrId = 99L;
        when(commonAccountRepository.findByAccountNo("0440000000099")).thenReturn(Optional.empty());
        when(contractRepository.findById(cntrId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issue(cntrId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(LoanErrorCode.LOAN_062);
                });

        verify(commonAccountRepository, never()).save(any());
    }

    @Test
    void 계좌번호_포맷_044_10자리패딩() {
        Long cntrId = 1L;
        when(commonAccountRepository.findByAccountNo("0440000000001")).thenReturn(Optional.empty());
        when(contractRepository.findById(cntrId)).thenReturn(Optional.of(contract(cntrId, 2L)));
        when(commonAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.issue(cntrId);

        ArgumentCaptor<CommonAccount> cap = ArgumentCaptor.forClass(CommonAccount.class);
        verify(commonAccountRepository).save(cap.capture());
        assertThat(cap.getValue().getAccountNo()).isEqualTo("0440000000001");
    }
}
