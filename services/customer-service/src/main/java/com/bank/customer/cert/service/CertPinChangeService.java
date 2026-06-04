package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.dto.CertPinChangeRequest;
import com.bank.customer.cert.repository.CertificateRepository;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CertPinChangeService {

    private final CertificateRepository certificateRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePin(CertPinChangeRequest request) {
        var cert = certificateRepository
                .findByCertificateSerialNumberAndDeletedAtIsNull(request.certSerialNumber())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_030));

        // 현재 PIN 검증 (certPinHash 없으면 로그인 비밀번호로 fallback)
        boolean valid;
        if (cert.getCertPinHash() != null) {
            valid = passwordEncoder.matches(request.currentPin(), cert.getCertPinHash());
        } else {
            var credential = credentialRepository
                    .findByCustomerIdAndDeletedAtIsNull(cert.getCustomerId())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));
            valid = passwordEncoder.matches(request.currentPin(), credential.getPasswordHash());
        }

        if (!valid) {
            throw new BusinessException(CustomerErrorCode.CUST_033);
        }

        cert.updatePinHash(passwordEncoder.encode(request.newPin()));
    }
}
