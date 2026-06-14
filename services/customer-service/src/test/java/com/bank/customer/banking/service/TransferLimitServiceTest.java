package com.bank.customer.banking.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.banking.domain.TransferLimit;
import com.bank.customer.banking.dto.ReduceTransferLimitRequest;
import com.bank.customer.banking.dto.TransferLimitResponse;
import com.bank.customer.banking.repository.TransferLimitRepository;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferLimitServiceTest {

    @Mock TransferLimitRepository transferLimitRepository;

    private TransferLimitService service;

    @BeforeEach
    void setUp() {
        service = new TransferLimitService(transferLimitRepository);
    }

    @Test
    @DisplayName("조회 — 행이 없으면 기본 100만/100만")
    void getLimit_default() {
        given(transferLimitRepository.findByCustomerIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        TransferLimitResponse res = service.getLimit(1L);

        assertThat(res.dailyLimit()).isEqualTo(1_000_000L);
        assertThat(res.onceLimit()).isEqualTo(1_000_000L);
    }

    @Test
    @DisplayName("조회 — 저장된 한도 반환")
    void getLimit_stored() {
        given(transferLimitRepository.findByCustomerIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(
                TransferLimit.builder().customerId(1L).dailyLimit(500_000L).onceLimit(300_000L).build()));

        TransferLimitResponse res = service.getLimit(1L);

        assertThat(res.dailyLimit()).isEqualTo(500_000L);
        assertThat(res.onceLimit()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("감액 — 현재보다 작은 값으로 정상 변경")
    void reduce_ok() {
        TransferLimit limit = TransferLimit.builder().customerId(1L).dailyLimit(1_000_000L).onceLimit(1_000_000L).build();
        given(transferLimitRepository.findByCustomerIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(limit));

        service.reduce(1L, new ReduceTransferLimitRequest(500_000L, 300_000L));

        assertThat(limit.getDailyLimit()).isEqualTo(500_000L);
        assertThat(limit.getOnceLimit()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("거부 — 증액(현재보다 큼)은 CUST_150")
    void reduce_rejectIncrease() {
        TransferLimit limit = TransferLimit.builder().customerId(1L).dailyLimit(1_000_000L).onceLimit(1_000_000L).build();
        given(transferLimitRepository.findByCustomerIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(limit));

        assertThatThrownBy(() -> service.reduce(1L, new ReduceTransferLimitRequest(2_000_000L, 1_000_000L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_150));
    }

    @Test
    @DisplayName("거부 — 0 이하는 CUST_151")
    void reduce_rejectNonPositive() {
        assertThatThrownBy(() -> service.reduce(1L, new ReduceTransferLimitRequest(0L, 0L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_151));
    }

    @Test
    @DisplayName("거부 — 1회 > 1일 은 CUST_152")
    void reduce_rejectOnceGtDaily() {
        assertThatThrownBy(() -> service.reduce(1L, new ReduceTransferLimitRequest(300_000L, 500_000L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_152));
    }
}
