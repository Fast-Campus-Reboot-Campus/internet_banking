package com.bank.customer.recovery.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.client.DepositClient;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.Party;
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

/**
 * 비로그인 ID 조회 / 사용자암호 재설정 — 계좌(번호+비밀번호) 본인확인 기반.
 * 계좌 자격 검증은 deposit-service 에 위임하고, 이름·loginId·credential 매칭은 여기서 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final String RT_KEY_PREFIX = "RT:";

    private final DepositClient          depositClient;
    private final CustomerRepository     customerRepository;
    private final PartyRepository        partyRepository;
    private final CredentialRepository   credentialRepository;
    private final PasswordEncoder        passwordEncoder;
    private final StringRedisTemplate    redisTemplate;

    /** 성명 + 계좌(번호·비밀번호)로 본인확인 후 로그인 ID 를 돌려준다. */
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest req) {
        Long customerId = depositClient.verifyAccountOwner(req.accountNumber(), req.accountPassword())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_098));

        String partyName = resolvePartyName(customerId);
        if (partyName == null || req.name() == null || !partyName.equals(req.name().trim())) {
            // 계좌 소유 고객명과 입력한 성명이 다름 → 본인확인 실패(정보 노출 방지로 동일 코드)
            throw new BusinessException(CustomerErrorCode.CUST_098);
        }

        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
        return new FindIdResponse(credential.getLoginId(), partyName);
    }

    /** 계좌 본인확인 후, 입력한 loginId 가 그 계좌 소유 고객의 것인지 확인하고 사용자암호를 재설정한다. */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        Long customerId = depositClient.verifyAccountOwner(req.accountNumber(), req.accountPassword())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_098));

        Credential credential = credentialRepository.findByLoginIdAndDeletedAtIsNull(req.loginId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_098));

        if (!credential.getCustomerId().equals(customerId)) {
            // 입력한 ID 가 계좌 소유 고객의 것이 아님 → 본인확인 실패
            throw new BusinessException(CustomerErrorCode.CUST_098);
        }

        credential.changePassword(passwordEncoder.encode(req.newPassword()));
        redisTemplate.delete(RT_KEY_PREFIX + customerId); // 기존 세션 무효화
        log.info("password reset via account verification: customerId={}", customerId);
    }

    private String resolvePartyName(Long customerId) {
        Customer customer = customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElse(null);
        if (customer == null) {
            return null;
        }
        Party party = partyRepository.findByPartyIdAndDeletedAtIsNull(customer.getPartyId()).orElse(null);
        return party != null ? party.getPartyName() : null;
    }
}
