package com.bank.customer.mobileauth.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.identity.domain.IdentityVerification;
import com.bank.customer.identity.repository.IdentityVerificationRepository;
import com.bank.customer.mobileauth.domain.MobileAuth;
import com.bank.customer.mobileauth.dto.SendMobileAuthRequest;
import com.bank.customer.mobileauth.dto.VerifyMobileAuthRequest;
import com.bank.customer.mobileauth.repository.MobileAuthRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileAuthService {

    private static final int    OTP_EXPIRY_MINUTES = 3;
    private static final int    MAX_ATTEMPTS       = 5;

    private final MobileAuthRepository           mobileAuthRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    /**
     * 인증 코드 발송.
     * MVP: 실제 SMS 발송 없이 로그로만 출력. 운영 환경에서는 SmsGateway Bean으로 교체.
     */
    @Transactional
    public Long send(SendMobileAuthRequest req, String ip, Long customerId) {
        String code = generateCode();

        log.info("[MobileAuth-MOCK] {} → {} (목적: {})", req.phoneNumber(), code, req.purposeCode());

        MobileAuth auth = mobileAuthRepository.save(MobileAuth.builder()
                .customerId(customerId)
                .mobileAuthMethodTypeCode(req.methodTypeCode())
                .mobileAuthTelecomCarrierCode(req.telecomCarrierCode())
                .mobileAuthRecipientPhoneNumber(req.phoneNumber())
                .mobileAuthCodeHash(sha256(code))
                .mobileAuthPurposeCode(req.purposeCode())
                .mobileAuthRequestIp(ip)
                .mobileAuthRequestChannelCode(MobileAuth.CHANNEL_WEB)
                .mobileAuthSentAt(OffsetDateTime.now())
                .mobileAuthExpiryAt(OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .mobileAuthVerifiedYn("F")
                .mobileAuthAttemptCount(0)
                .build());

        return auth.getMobileAuthId();
    }

    /**
     * 인증 코드 검증.
     * 목적이 IDENTITY_VERIFY이면 IdentityVerification 이력도 함께 저장.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(VerifyMobileAuthRequest req, Long customerId) {
        MobileAuth auth = mobileAuthRepository
                .findTopByMobileAuthRecipientPhoneNumberAndMobileAuthPurposeCodeAndMobileAuthVerifiedYnOrderByMobileAuthSentAtDesc(
                        req.phoneNumber(), req.purposeCode(), "F")
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_090));

        if (auth.isExpired()) {
            throw new BusinessException(CustomerErrorCode.CUST_091);
        }
        if (auth.isVerified()) {
            throw new BusinessException(CustomerErrorCode.CUST_093);
        }
        if (auth.getMobileAuthAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException(CustomerErrorCode.CUST_091);
        }

        auth.recordAttempt();

        if (!sha256(req.code()).equals(auth.getMobileAuthCodeHash())) {
            auth.fail("WRONG_CODE");
            throw new BusinessException(CustomerErrorCode.CUST_092);
        }

        auth.verify();

        // 본인확인 목적이면 이력 저장 (MVP: CI값은 전화번호 해시로 대체)
        if (MobileAuth.PURPOSE_IDENTITY_VERIFY.equals(req.purposeCode())) {
            identityVerificationRepository.save(IdentityVerification.builder()
                    .customerId(customerId)
                    .mobileAuthId(auth.getMobileAuthId())
                    .identityVerificationAgencyCode(IdentityVerification.AGENCY_NICE)
                    .identityVerificationPurposeCode(req.purposeCode())
                    .identityVerificationCiValue(sha256(req.phoneNumber()))
                    .identityVerificationName("N/A")
                    .identityVerificationBirthDate("00000000")
                    .identityVerificationGenderCode("0")
                    .identityVerificationNationalityTypeCode("DOMESTIC")
                    .identityVerificationTelecomCarrierCode(auth.getMobileAuthTelecomCarrierCode())
                    .identityVerificationPhoneNumber(req.phoneNumber())
                    .identityVerifiedAt(OffsetDateTime.now())
                    .build());
        }
    }

    private static String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
