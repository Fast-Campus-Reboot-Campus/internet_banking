package com.bank.customer.settings.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.domain.CustomerStatusHistory;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.customer.repository.CustomerStatusHistoryRepository;
import com.bank.customer.fds.domain.FdsDetection;
import com.bank.customer.fds.service.FdsService;
import com.bank.customer.history.domain.PasswordHistory;
import com.bank.customer.history.repository.PasswordHistoryRepository;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.settings.dto.ChangePasswordRequest;
import com.bank.customer.settings.dto.SettingsResponse;
import com.bank.customer.settings.dto.UpdateNotificationRequest;
import com.bank.customer.settings.dto.UpdateProfileRequest;
import com.bank.customer.settings.dto.WithdrawRequest;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String RT_KEY_PREFIX    = "RT:";
    private static final String CHANNEL_WEB      = "WEB";
    private static final String REASON_USER_REQ  = "USER_REQUEST";

    private final CustomerRepository               customerRepository;
    private final PartyRepository                  partyRepository;
    private final CredentialRepository             credentialRepository;
    private final CustomerStatusHistoryRepository  customerStatusHistoryRepository;
    private final PasswordHistoryRepository        passwordHistoryRepository;
    private final PasswordEncoder                  passwordEncoder;
    private final StringRedisTemplate              redisTemplate;
    private final FdsService                       fdsService;

    @Transactional(readOnly = true)
    public SettingsResponse getSettings(Long customerId) {
        Customer customer = findActiveCustomer(customerId);
        Party party = partyRepository.findByPartyIdAndDeletedAtIsNull(customer.getPartyId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        return new SettingsResponse(
                party.getPartyName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getZipCode(),
                customer.getAddress(),
                customer.getAddressDetail(),
                "T".equals(customer.getSmsReceiveYn()),
                "T".equals(customer.getEmailReceiveYn()),
                "T".equals(customer.getPostalReceiveYn())
        );
    }

    @Transactional
    public void updateProfile(Long customerId, UpdateProfileRequest req) {
        Customer customer = findActiveCustomer(customerId);
        customer.updateContact(req.email(), req.phone(), req.zipCode(), req.address(), req.addressDetail());
    }

    @Transactional
    public void updateNotification(Long customerId, UpdateNotificationRequest req) {
        Customer customer = findActiveCustomer(customerId);
        customer.updateNotification(
                req.smsReceiveYn() ? "T" : "F",
                req.emailReceiveYn() ? "T" : "F",
                req.postalReceiveYn() ? "T" : "F",
                customer.getNotificationMethodCode()
        );
    }

    @Transactional
    public void changePassword(Long customerId, ChangePasswordRequest req, String ip) {
        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!passwordEncoder.matches(req.currentPassword(), credential.getPasswordHash())) {
            throw new BusinessException(CustomerErrorCode.CUST_020);
        }

        // 변경 전 해시를 이력에 먼저 저장
        PasswordHistory history = passwordHistoryRepository.save(PasswordHistory.builder()
                .credentialId(credential.getCredentialId())
                .customerId(customerId)
                .passwordHash(credential.getPasswordHash())
                .passwordChangeChannelCode(CHANNEL_WEB)
                .passwordChangeReasonCode(REASON_USER_REQ)
                .passwordChangeIp(ip)
                .build());

        credential.changePassword(passwordEncoder.encode(req.newPassword()));
        redisTemplate.delete(RT_KEY_PREFIX + customerId);

        // FDS 평가 — 비밀번호 잦은 변경 모니터링(PWD_CHANGE_MONITOR_3 등).
        // MONITOR 룰이라 탐지만 기록하고 변경 자체는 막지 않는다.
        evaluateFdsSilently(customerId, history.getPasswordHistoryId());
    }

    /** 비밀번호 변경 FDS 평가. BLOCK 룰이 발동해도 변경은 막지 않고 로그만 남긴다(모니터링). */
    private void evaluateFdsSilently(Long customerId, Long referenceId) {
        try {
            fdsService.evaluate(customerId, FdsDetection.EVENT_PASSWORD_CHANGE, referenceId);
        } catch (BusinessException e) {
            log.warn("FDS BLOCK 발동 (비밀번호 변경 경로): customerId={}", customerId);
        }
    }

    @Transactional
    public void withdraw(Long customerId, WithdrawRequest req) {
        Customer customer = findActiveCustomer(customerId);

        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!passwordEncoder.matches(req.currentPassword(), credential.getPasswordHash())) {
            throw new BusinessException(CustomerErrorCode.CUST_020);
        }

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        customer.close(now, "CUST_WITHDRAW");
        credential.close();
        redisTemplate.delete(RT_KEY_PREFIX + customerId);

        customerStatusHistoryRepository.save(
                CustomerStatusHistory.ofTransition(
                        customerId, null,
                        Customer.STATUS_ACTIVE, Customer.STATUS_CLOSED,
                        CustomerStatusHistory.REASON_CUST_REQ,
                        "고객 자발적 해지",
                        now, false, null));  // 고객 본인 해지 — 직원 행위자 없음
    }

    private Customer findActiveCustomer(Long customerId) {
        Customer customer = customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
        if (!customer.isActive()) {
            throw new BusinessException(CustomerErrorCode.CUST_012);
        }
        return customer;
    }
}
