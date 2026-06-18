package com.bank.customer.banking.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.banking.domain.WithdrawalAccount;
import com.bank.customer.banking.dto.RegisterWithdrawalAccountRequest;
import com.bank.customer.banking.dto.UpdateOrderRequest;
import com.bank.customer.banking.dto.WithdrawalAccountResponse;
import com.bank.customer.banking.repository.WithdrawalAccountRepository;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WithdrawalAccountServiceTest {

    @Mock WithdrawalAccountRepository withdrawalAccountRepository;

    private WithdrawalAccountService service;

    @BeforeEach
    void setUp() {
        service = new WithdrawalAccountService(withdrawalAccountRepository);
    }

    private RegisterWithdrawalAccountRequest req(String bankCode, String accountNumber) {
        return new RegisterWithdrawalAccountRequest(accountNumber, bankCode, "신한은행", "홍길동", "월급통장");
    }

    private WithdrawalAccount account(Long id, short order) {
        return WithdrawalAccount.builder()
                .withdrawalAccountId(id)
                .customerId(1L)
                .accountNumber("110200300400")
                .bankCode("088")
                .bankName("신한은행")
                .registrationType("ONLINE")
                .priorityOrder(order)
                .registeredAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("등록 거부 — 본행(AXFUL) 계좌는 CUST_052 (대소문자 무시)")
    void register_rejectHomeBank() {
        assertThatThrownBy(() -> service.register(1L, req("axful", "110200300400")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_052));
    }

    @Test
    @DisplayName("등록 거부 — 동일 계좌번호 중복은 CUST_051")
    void register_rejectDuplicate() {
        given(withdrawalAccountRepository.existsByCustomerIdAndAccountNumberAndDeletedAtIsNull(1L, "110200300400"))
                .willReturn(true);

        assertThatThrownBy(() -> service.register(1L, req("088", "110200300400")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_051));
    }

    @Test
    @DisplayName("등록 — 우선순위는 기존 등록 수를 기본값으로 부여")
    void register_priorityOrderFromCount() {
        given(withdrawalAccountRepository.existsByCustomerIdAndAccountNumberAndDeletedAtIsNull(1L, "110200300400"))
                .willReturn(false);
        given(withdrawalAccountRepository.findByCustomerIdAndDeletedAtIsNullOrderByPriorityOrderAsc(1L))
                .willReturn(List.of(account(10L, (short) 0), account(11L, (short) 1)));
        given(withdrawalAccountRepository.save(any(WithdrawalAccount.class)))
                .willAnswer(inv -> inv.getArgument(0));

        WithdrawalAccountResponse res = service.register(1L, req("088", "110200300400"));

        assertThat(res.priorityOrder()).isEqualTo((short) 2);
        assertThat(res.registrationType()).isEqualTo("ONLINE");
    }

    @Test
    @DisplayName("삭제 — 대상 없으면 CUST_050")
    void delete_notFound() {
        given(withdrawalAccountRepository.findByWithdrawalAccountIdAndCustomerIdAndDeletedAtIsNull(99L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_050));
    }

    @Test
    @DisplayName("순서변경 — 전달된 ID 순서대로 priorityOrder 재부여")
    void updateOrder_reassigns() {
        WithdrawalAccount a = account(10L, (short) 0);
        WithdrawalAccount b = account(11L, (short) 1);
        given(withdrawalAccountRepository.findByCustomerIdAndDeletedAtIsNullOrderByPriorityOrderAsc(1L))
                .willReturn(List.of(a, b));

        service.updateOrder(1L, new UpdateOrderRequest(List.of(11L, 10L)));

        assertThat(b.getPriorityOrder()).isEqualTo((short) 0);
        assertThat(a.getPriorityOrder()).isEqualTo((short) 1);
    }
}
