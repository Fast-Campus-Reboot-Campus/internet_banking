package com.bank.customer.recovery.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.identity.domain.IdentityVerification;
import com.bank.customer.identity.repository.IdentityVerificationRepository;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.repository.PartyPersonRepository;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.recovery.dto.FindIdRequest;
import com.bank.customer.recovery.dto.FindIdResponse;
import com.bank.customer.recovery.dto.ResetPasswordRequest;
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

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountRecoveryServiceTest {

    @Mock IdentityVerificationRepository identityVerificationRepository;
    @Mock PartyPersonRepository          partyPersonRepository;
    @Mock PartyRepository                partyRepository;
    @Mock CustomerRepository             customerRepository;
    @Mock CredentialRepository           credentialRepository;
    @Mock PasswordEncoder                passwordEncoder;
    @Mock StringRedisTemplate            redisTemplate;

    private AccountRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new AccountRecoveryService(identityVerificationRepository, partyPersonRepository,
                partyRepository, customerRepository, credentialRepository, passwordEncoder, redisTemplate);
    }

    private IdentityVerification validIdentity(String purpose, String ci) {
        IdentityVerification iv = mock(IdentityVerification.class);
        given(iv.getIdentityVerificationPurposeCode()).willReturn(purpose);
        given(iv.isConsumed()).willReturn(false);
        given(iv.getIdentityVerifiedAt()).willReturn(OffsetDateTime.now());
        given(iv.getIdentityVerificationCiValue()).willReturn(ci);
        return iv;
    }

    /** CI → PartyPerson → 고객 → credential → party 체인을 정상 배선한다. */
    private void wireCustomer(String ci, Long customerId, Long partyId, String name, Credential credential) {
        PartyPerson pp = mock(PartyPerson.class);
        given(pp.getPartyId()).willReturn(partyId);
        given(partyPersonRepository.findByCiValueAndDeletedAtIsNull(ci)).willReturn(Optional.of(pp));

        Customer c = mock(Customer.class);
        given(c.getCustomerId()).willReturn(customerId);
        given(c.getPartyId()).willReturn(partyId);
        given(customerRepository.findByPartyIdAndCustomerStatusCodeNotAndDeletedAtIsNull(eq(partyId), any()))
                .willReturn(Optional.of(c));

        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(customerId)).willReturn(Optional.of(credential));

        Party party = mock(Party.class);
        given(party.getPartyName()).willReturn(name);
        given(partyRepository.findByPartyIdAndDeletedAtIsNull(partyId)).willReturn(Optional.of(party));
    }

    @Test
    @DisplayName("ID 조회 — 본인확인 통과 시 loginId·고객명 반환")
    void findId_success() {
        IdentityVerification iv = validIdentity("IDENTITY_VERIFY", "CI-1");
        given(identityVerificationRepository.findById(10L)).willReturn(Optional.of(iv));
        Credential credential = mock(Credential.class);
        given(credential.getLoginId()).willReturn("user01");
        wireCustomer("CI-1", 1L, 100L, "테스트고객", credential);

        FindIdResponse res = service.findId(new FindIdRequest(10L));

        assertThat(res.loginId()).isEqualTo("user01");
        assertThat(res.customerName()).isEqualTo("테스트고객");
    }

    @Test
    @DisplayName("ID 조회 — 본인확인 이력 없음/잘못된 목적이면 CUST_094")
    void findId_invalidVerification() {
        given(identityVerificationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findId(new FindIdRequest(99L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_094));
    }

    @Test
    @DisplayName("ID 조회 — 검증된 CI 에 해당 고객 없으면 CUST_094 (존재여부 비노출)")
    void findId_noCustomerForCi() {
        IdentityVerification iv = validIdentity("IDENTITY_VERIFY", "CI-X");
        given(identityVerificationRepository.findById(10L)).willReturn(Optional.of(iv));
        given(partyPersonRepository.findByCiValueAndDeletedAtIsNull("CI-X")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findId(new FindIdRequest(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_094));
    }

    @Test
    @DisplayName("암호 재설정 — 본인확인 통과 시 비번 변경·본인확인 소비·세션 무효화")
    void resetPassword_success() {
        IdentityVerification iv = validIdentity("PASSWORD_RESET", "CI-1");
        given(identityVerificationRepository.findById(10L)).willReturn(Optional.of(iv));
        Credential credential = mock(Credential.class);
        wireCustomer("CI-1", 1L, 100L, "테스트고객", credential);
        given(passwordEncoder.encode("Newpass1!")).willReturn("$2a$new");

        service.resetPassword(new ResetPasswordRequest(10L, "Newpass1!"));

        verify(credential).changePassword("$2a$new");
        verify(iv).consume(1L);
        verify(redisTemplate).delete("RT:1");
    }

    @Test
    @DisplayName("암호 재설정 — 이미 소비된 본인확인이면 CUST_096, 비번 변경 안 함")
    void resetPassword_consumed() {
        IdentityVerification iv = mock(IdentityVerification.class);
        given(iv.getIdentityVerificationPurposeCode()).willReturn("PASSWORD_RESET");
        given(iv.isConsumed()).willReturn(true);
        given(identityVerificationRepository.findById(10L)).willReturn(Optional.of(iv));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(10L, "Newpass1!")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_096));
    }
}
