package com.bank.customer.pin.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.AuthMethod;
import com.bank.customer.cert.repository.AuthMethodRepository;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.device.domain.RegisteredDevice;
import com.bank.customer.device.repository.RegisteredDeviceRepository;
import com.bank.customer.login.service.AuthEventService;
import com.bank.customer.pin.domain.Pin;
import com.bank.customer.pin.dto.RegisterPinRequest;
import com.bank.customer.pin.repository.PinRepository;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PinServiceTest {

    @Mock PinRepository              pinRepository;
    @Mock CredentialRepository       credentialRepository;
    @Mock CustomerRepository         customerRepository;
    @Mock RegisteredDeviceRepository deviceRepository;
    @Mock AuthMethodRepository       authMethodRepository;
    @Mock PasswordEncoder            passwordEncoder;
    @Mock AuthEventService           authEventService;

    private PinService pinService;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long DEVICE_ID   = 9L;

    private PinService newService() {
        return new PinService(
                pinRepository, credentialRepository, customerRepository,
                deviceRepository, authMethodRepository, passwordEncoder,
                authEventService);
    }

    private void givenValidPreconditions() {
        Credential credential = org.mockito.Mockito.mock(Credential.class);
        given(credential.getPasswordHash()).willReturn("hashedpw");
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(CUSTOMER_ID))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("pw1234!!", "hashedpw")).willReturn(true);

        RegisteredDevice device = org.mockito.Mockito.mock(RegisteredDevice.class);
        given(device.isActive()).willReturn(true);
        given(deviceRepository.findByDeviceIdAndCustomerIdAndDeletedAtIsNull(DEVICE_ID, CUSTOMER_ID))
                .willReturn(Optional.of(device));

        given(pinRepository.existsByCustomerIdAndDeviceIdAndDeletedAtIsNull(CUSTOMER_ID, DEVICE_ID))
                .willReturn(false);
        given(passwordEncoder.encode("123456")).willReturn("hashedpin");
    }

    @Test
    @DisplayName("PIN 등록 — 기존 PIN 인증수단이 없으면 PIN 타입 인증수단을 새로 생성해 연결한다")
    void registerCreatesPinAuthMethodWhenNoneExists() {
        pinService = newService();
        givenValidPreconditions();

        // 활성 인증수단 없음 → 신규 생성 경로
        given(authMethodRepository.findByCustomerIdAndAuthMethodStatusCodeAndDeletedAtIsNull(
                CUSTOMER_ID, AuthMethod.STATUS_ACTIVE)).willReturn(List.of());
        given(authMethodRepository.save(any(AuthMethod.class)))
                .willReturn(AuthMethod.builder().authMethodId(55L).build());

        pinService.register(CUSTOMER_ID, new RegisterPinRequest(DEVICE_ID, "123456", "pw1234!!"));

        // 생성된 인증수단은 PIN 타입·ACTIVE
        ArgumentCaptor<AuthMethod> amCaptor = ArgumentCaptor.forClass(AuthMethod.class);
        verify(authMethodRepository).save(amCaptor.capture());
        assertThat(amCaptor.getValue().getAuthMethodTypeCode()).isEqualTo(AuthMethod.TYPE_PIN);
        assertThat(amCaptor.getValue().getAuthMethodStatusCode()).isEqualTo(AuthMethod.STATUS_ACTIVE);

        // PIN은 새로 만든 인증수단 id로 연결
        ArgumentCaptor<Pin> pinCaptor = ArgumentCaptor.forClass(Pin.class);
        verify(pinRepository).save(pinCaptor.capture());
        assertThat(pinCaptor.getValue().getAuthMethodId()).isEqualTo(55L);
        assertThat(pinCaptor.getValue().getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(pinCaptor.getValue().getPinHash()).isEqualTo("hashedpin");
    }

    @Test
    @DisplayName("PIN 등록 — 기존 PIN 인증수단이 있으면 재사용하고 새로 만들지 않는다")
    void registerReusesExistingPinAuthMethod() {
        pinService = newService();
        givenValidPreconditions();

        AuthMethod existing = AuthMethod.builder()
                .authMethodId(77L)
                .authMethodTypeCode(AuthMethod.TYPE_PIN)
                .authMethodStatusCode(AuthMethod.STATUS_ACTIVE)
                .build();
        given(authMethodRepository.findByCustomerIdAndAuthMethodStatusCodeAndDeletedAtIsNull(
                CUSTOMER_ID, AuthMethod.STATUS_ACTIVE)).willReturn(List.of(existing));

        pinService.register(CUSTOMER_ID, new RegisterPinRequest(DEVICE_ID, "123456", "pw1234!!"));

        verify(authMethodRepository, never()).save(any());
        ArgumentCaptor<Pin> pinCaptor = ArgumentCaptor.forClass(Pin.class);
        verify(pinRepository).save(pinCaptor.capture());
        assertThat(pinCaptor.getValue().getAuthMethodId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("PIN 등록 실패 — 현재 비밀번호 불일치")
    void registerFailsWhenPasswordMismatch() {
        pinService = newService();
        Credential credential = org.mockito.Mockito.mock(Credential.class);
        given(credential.getPasswordHash()).willReturn("hashedpw");
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(CUSTOMER_ID))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() ->
                pinService.register(CUSTOMER_ID, new RegisterPinRequest(DEVICE_ID, "123456", "wrong")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CustomerErrorCode.CUST_020));

        verify(pinRepository, never()).save(any());
        verify(authMethodRepository, never()).save(any());
    }
}
