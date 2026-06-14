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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 비로그인 ID 조회 / 사용자암호 재설정 — 휴대폰 본인확인(주민번호+휴대폰) 기반.
 * 가입(online-join)과 동일하게 {@code /api/v1/mobile-auth} 로 받은 verificationId 로 본인확인하고,
 * CI 값으로 기존 고객(PartyPerson→party→customer→credential)을 찾는다. 계좌 정보는 쓰지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final String RT_KEY_PREFIX = "RT:";
    private static final long   VERIFICATION_VALIDITY_MINUTES = 30;

    private final IdentityVerificationRepository identityVerificationRepository;
    private final PartyPersonRepository          partyPersonRepository;
    private final PartyRepository                partyRepository;
    private final CustomerRepository             customerRepository;
    private final CredentialRepository           credentialRepository;
    private final PasswordEncoder                passwordEncoder;
    private final StringRedisTemplate            redisTemplate;

    /** 본인확인 후 로그인 ID 반환. (소비하지 않음 — 이어서 같은 본인확인으로 재설정 가능) */
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest req) {
        IdentityVerification identity = loadVerifiedIdentity(req.verificationId());
        Customer customer = resolveCustomer(identity.getIdentityVerificationCiValue());
        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customer.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_094));
        return new FindIdResponse(credential.getLoginId(), resolvePartyName(customer.getPartyId()));
    }

    /** 본인확인 후 사용자암호 재설정. 본인확인 1건은 재설정 1건에만 사용(소비). */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        IdentityVerification identity = loadVerifiedIdentity(req.verificationId());
        Customer customer = resolveCustomer(identity.getIdentityVerificationCiValue());
        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customer.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_094));

        credential.changePassword(passwordEncoder.encode(req.newPassword()));
        identity.consume(customer.getCustomerId());
        redisTemplate.delete(RT_KEY_PREFIX + customer.getCustomerId()); // 기존 세션 무효화
        log.info("password reset via mobile-auth: customerId={}", customer.getCustomerId());
    }

    /** verificationId 의 본인확인 이력 검증 — 목적(IDENTITY_VERIFY/PASSWORD_RESET)·소비·만료. */
    private IdentityVerification loadVerifiedIdentity(Long verificationId) {
        IdentityVerification identity = identityVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_094));

        String purpose = identity.getIdentityVerificationPurposeCode();
        if (!IdentityVerification.PURPOSE_IDENTITY_VERIFY.equals(purpose)
                && !IdentityVerification.PURPOSE_PASSWORD_RESET.equals(purpose)) {
            throw new BusinessException(CustomerErrorCode.CUST_094);
        }
        if (identity.isConsumed()) {
            throw new BusinessException(CustomerErrorCode.CUST_096);
        }
        if (identity.getIdentityVerifiedAt().plusMinutes(VERIFICATION_VALIDITY_MINUTES)
                .isBefore(OffsetDateTime.now())) {
            throw new BusinessException(CustomerErrorCode.CUST_095);
        }
        return identity;
    }

    /** CI 값으로 사람(PartyPerson) → party → 해지되지 않은 고객을 찾는다. */
    private Customer resolveCustomer(String ciValue) {
        PartyPerson person = partyPersonRepository.findByCiValueAndDeletedAtIsNull(ciValue)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_094));
        return customerRepository
                .findByPartyIdAndCustomerStatusCodeNotAndDeletedAtIsNull(person.getPartyId(), Customer.STATUS_CLOSED)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_094));
    }

    private String resolvePartyName(Long partyId) {
        Party party = partyRepository.findByPartyIdAndDeletedAtIsNull(partyId).orElse(null);
        return party != null ? party.getPartyName() : null;
    }
}
