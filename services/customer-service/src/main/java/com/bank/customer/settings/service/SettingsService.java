package com.bank.customer.settings.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.settings.dto.ChangePasswordRequest;
import com.bank.customer.settings.dto.SettingsResponse;
import com.bank.customer.settings.dto.UpdateNotificationRequest;
import com.bank.customer.settings.dto.UpdateProfileRequest;
import com.bank.customer.settings.dto.WithdrawRequest;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String RT_KEY_PREFIX = "RT:";

    private final CustomerRepository customerRepository;
    private final PartyRepository partyRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

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
    public void changePassword(Long customerId, ChangePasswordRequest req) {
        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!passwordEncoder.matches(req.currentPassword(), credential.getPasswordHash())) {
            throw new BusinessException(CustomerErrorCode.CUST_020);
        }

        credential.changePassword(passwordEncoder.encode(req.newPassword()));
        redisTemplate.delete(RT_KEY_PREFIX + customerId);
    }

    @Transactional
    public void withdraw(Long customerId, WithdrawRequest req) {
        Customer customer = findActiveCustomer(customerId);

        Credential credential = credentialRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!passwordEncoder.matches(req.currentPassword(), credential.getPasswordHash())) {
            throw new BusinessException(CustomerErrorCode.CUST_020);
        }

        customer.close(java.time.OffsetDateTime.now(), "CUST_WITHDRAW");
        credential.close();
        redisTemplate.delete(RT_KEY_PREFIX + customerId);
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
