package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.AuthMethod;
import com.bank.customer.cert.dto.AuthMethodResponse;
import com.bank.customer.cert.repository.AuthMethodRepository;
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
class AuthMethodServiceTest {

    @Mock AuthMethodRepository authMethodRepository;

    private AuthMethodService service;

    @BeforeEach
    void setUp() {
        service = new AuthMethodService(authMethodRepository);
    }

    private AuthMethod method(Long id, String status, boolean primary) {
        return AuthMethod.builder()
                .authMethodId(id)
                .customerId(1L)
                .authMethodTypeCode(AuthMethod.TYPE_PIN)
                .authMethodStatusCode(status)
                .primaryAuthMethodYn(primary ? "T" : "F")
                .authMethodRegisteredDate("20240101")
                .build();
    }

    @Test
    @DisplayName("별칭변경 — 대상 없으면 CUST_130")
    void updateAlias_notFound() {
        given(authMethodRepository.findByAuthMethodIdAndCustomerIdAndDeletedAtIsNull(9L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAlias(1L, 9L, "새별칭"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_130));
    }

    @Test
    @DisplayName("주 인증수단 변경 — 기존 주 수단을 해제하고 새 수단을 지정")
    void setPrimary_swapsPrevious() {
        AuthMethod prev = method(10L, AuthMethod.STATUS_ACTIVE, true);
        AuthMethod next = method(11L, AuthMethod.STATUS_ACTIVE, false);
        given(authMethodRepository.findByCustomerIdAndPrimaryAuthMethodYnAndDeletedAtIsNull(1L, "T"))
                .willReturn(Optional.of(prev));
        given(authMethodRepository.findByAuthMethodIdAndCustomerIdAndDeletedAtIsNull(11L, 1L))
                .willReturn(Optional.of(next));

        AuthMethodResponse res = service.setPrimary(1L, 11L);

        assertThat(prev.isPrimary()).isFalse();
        assertThat(next.isPrimary()).isTrue();
        assertThat(res.primary()).isTrue();
    }

    @Test
    @DisplayName("주 인증수단 변경 — 비활성 수단은 CUST_130")
    void setPrimary_inactiveRejected() {
        AuthMethod inactive = method(11L, AuthMethod.STATUS_INACTIVE, false);
        given(authMethodRepository.findByCustomerIdAndPrimaryAuthMethodYnAndDeletedAtIsNull(1L, "T"))
                .willReturn(Optional.empty());
        given(authMethodRepository.findByAuthMethodIdAndCustomerIdAndDeletedAtIsNull(11L, 1L))
                .willReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.setPrimary(1L, 11L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_130));
    }

    @Test
    @DisplayName("해제 — 주 인증수단은 해제 불가 CUST_131")
    void deactivate_primaryRejected() {
        AuthMethod primary = method(10L, AuthMethod.STATUS_ACTIVE, true);
        given(authMethodRepository.findByAuthMethodIdAndCustomerIdAndDeletedAtIsNull(10L, 1L))
                .willReturn(Optional.of(primary));

        assertThatThrownBy(() -> service.deactivate(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_131));
    }

    @Test
    @DisplayName("해제 — 비주(non-primary) 수단은 INACTIVE 로 전이")
    void deactivate_ok() {
        AuthMethod method = method(11L, AuthMethod.STATUS_ACTIVE, false);
        given(authMethodRepository.findByAuthMethodIdAndCustomerIdAndDeletedAtIsNull(11L, 1L))
                .willReturn(Optional.of(method));

        service.deactivate(1L, 11L);

        assertThat(method.isActive()).isFalse();
    }
}
