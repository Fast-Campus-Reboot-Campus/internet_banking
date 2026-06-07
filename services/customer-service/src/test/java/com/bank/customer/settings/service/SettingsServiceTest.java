package com.bank.customer.settings.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.customer.repository.CustomerStatusHistoryRepository;
import com.bank.customer.fds.domain.FdsDetection;
import com.bank.customer.fds.service.FdsService;
import com.bank.customer.history.domain.PasswordHistory;
import com.bank.customer.history.repository.PasswordHistoryRepository;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.settings.dto.ChangePasswordRequest;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 비밀번호 변경 후처리 검증의 핵심:
 * 변경 성공 시 PASSWORD_CHANGE FDS 평가가 실제로 호출되어야 한다
 * (PWD_CHANGE_MONITOR_3 룰 트리거). 현재 비밀번호 불일치 시에는 호출되지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettingsServiceTest {

    @Mock CustomerRepository              customerRepository;
    @Mock PartyRepository                 partyRepository;
    @Mock CredentialRepository            credentialRepository;
    @Mock CustomerStatusHistoryRepository customerStatusHistoryRepository;
    @Mock PasswordHistoryRepository       passwordHistoryRepository;
    @Mock PasswordEncoder                 passwordEncoder;
    @Mock StringRedisTemplate             redisTemplate;
    @Mock FdsService                      fdsService;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(
                customerRepository, partyRepository, credentialRepository,
                customerStatusHistoryRepository, passwordHistoryRepository,
                passwordEncoder, redisTemplate, fdsService);

        PasswordHistory saved = mock(PasswordHistory.class);
        given(saved.getPasswordHistoryId()).willReturn(500L);
        given(passwordHistoryRepository.save(any())).willReturn(saved);
    }

    @Test
    @DisplayName("비밀번호 변경 성공 — 이력 저장 + PASSWORD_CHANGE FDS 평가 호출")
    void changePassword_evaluatesFds() {
        Credential credential = mockCredential();
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(eq("old-pw"), any())).willReturn(true);
        given(passwordEncoder.encode("new-pw")).willReturn("$2a$new");

        settingsService.changePassword(1L,
                new ChangePasswordRequest("old-pw", "new-pw"), "127.0.0.1");

        verify(passwordHistoryRepository).save(any());
        verify(fdsService).evaluate(eq(1L), eq(FdsDetection.EVENT_PASSWORD_CHANGE), eq(500L));
    }

    @Test
    @DisplayName("비밀번호 변경 — FDS BLOCK 발동해도 변경은 막지 않는다(silent)")
    void changePassword_fdsBlockIsSilent() {
        Credential credential = mockCredential();
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(eq("old-pw"), any())).willReturn(true);
        given(passwordEncoder.encode("new-pw")).willReturn("$2a$new");
        org.mockito.BDDMockito.willThrow(new BusinessException(CustomerErrorCode.CUST_060))
                .given(fdsService).evaluate(anyLong(), eq(FdsDetection.EVENT_PASSWORD_CHANGE), anyLong());

        // 예외가 전파되지 않아야 한다
        settingsService.changePassword(1L,
                new ChangePasswordRequest("old-pw", "new-pw"), "127.0.0.1");

        verify(passwordHistoryRepository).save(any());
    }

    @Test
    @DisplayName("현재 비밀번호 불일치 — 이력/FDS 평가 모두 호출하지 않고 CUST_020")
    void changePassword_wrongCurrentPassword_noFds() {
        Credential credential = mockCredential();
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(eq("wrong"), any())).willReturn(false);

        assertThatThrownBy(() -> settingsService.changePassword(1L,
                new ChangePasswordRequest("wrong", "new-pw"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CustomerErrorCode.CUST_020));

        verify(passwordHistoryRepository, never()).save(any());
        verify(fdsService, never()).evaluate(anyLong(), any(), anyLong());
    }

    private Credential mockCredential() {
        Credential c = mock(Credential.class);
        given(c.getCredentialId()).willReturn(10L);
        given(c.getPasswordHash()).willReturn("$2a$old");
        return c;
    }
}
