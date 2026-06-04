package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.Certificate;
import com.bank.customer.cert.dto.CertLoginRequest;
import com.bank.customer.cert.repository.CertificateRepository;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.fds.domain.FdsDetection;
import com.bank.customer.fds.service.FdsService;
import com.bank.customer.history.domain.CertificateUse;
import com.bank.customer.history.repository.CertificateUseRepository;
import com.bank.customer.login.config.EmployeeDirectoryProperties;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.support.CustomerErrorCode;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertLoginService {

    private static final String RT_KEY_PREFIX = "RT:";

    private final CertificateRepository     certificateRepository;
    private final CredentialRepository      credentialRepository;
    private final CustomerRepository        customerRepository;
    private final CertificateUseRepository  certificateUseRepository;
    private final PasswordEncoder           passwordEncoder;
    private final JwtProvider               jwtProvider;
    private final JwtProperties             jwtProperties;
    private final StringRedisTemplate       redisTemplate;
    private final EmployeeDirectoryProperties employeeDirectory;
    private final FdsService                fdsService;

    /**
     * 인증서 로그인.
     * PIN 검증은 MVP 단계에서 credential.passwordHash 로 위임한다.
     * 성공/실패 모두 certificate_use 이력에 기록한다.
     */
    @SuppressWarnings("null")
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse certLogin(CertLoginRequest request, String ip) {

        Certificate cert = certificateRepository
                .findByCertificateSerialNumberAndDeletedAtIsNull(request.certSerialNumber())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_030));

        try {
            if (cert.isLocked()) {
                throw new BusinessException(CustomerErrorCode.CUST_034);
            }
            if (!cert.isActive()) {
                if (Certificate.STATUS_REVOKED.equals(cert.getCertificateStatusCode())) {
                    throw new BusinessException(CustomerErrorCode.CUST_032);
                }
                throw new BusinessException(CustomerErrorCode.CUST_031);
            }
            if (cert.isExpired()) {
                throw new BusinessException(CustomerErrorCode.CUST_031);
            }

            boolean pinValid;
            if (cert.getCertPinHash() != null) {
                pinValid = passwordEncoder.matches(request.pin(), cert.getCertPinHash());
            } else {
                var credential = credentialRepository
                        .findByCustomerIdAndDeletedAtIsNull(cert.getCustomerId())
                        .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));
                pinValid = passwordEncoder.matches(request.pin(), credential.getPasswordHash());
            }

            if (!pinValid) {
                cert.recordLoginFailure();
                String resultCode = cert.isLocked()
                        ? CertificateUse.RESULT_FAIL_LOCKED
                        : CertificateUse.RESULT_FAIL_PIN;
                CertificateUse use = saveCertUse(cert, ip, resultCode,
                        cert.isLocked() ? CustomerErrorCode.CUST_034.getCode()
                                        : CustomerErrorCode.CUST_033.getCode());
                // 인증서 실패 누적 FDS 평가 — BLOCK 룰(CERT_FAIL_BLOCK_5) 발동 시 CUST_060 으로 차단
                fdsService.evaluate(cert.getCustomerId(), FdsDetection.EVENT_CERT_LOGIN, use.getCertificateUseId());
                throw new BusinessException(
                        cert.isLocked() ? CustomerErrorCode.CUST_034 : CustomerErrorCode.CUST_033);
            }

            Customer customer = customerRepository
                    .findByCustomerIdAndDeletedAtIsNull(cert.getCustomerId())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

            if (!customer.isActive()) {
                throw new BusinessException(CustomerErrorCode.CUST_012);
            }

            cert.recordLoginSuccess();
            CertificateUse use = saveCertUse(cert, ip, CertificateUse.RESULT_SUCCESS, null);
            // 성공 경로에서도 FDS 평가 — BLOCK 발동해도 로그인은 막지 않고 로그만 남긴다(모니터링)
            evaluateFdsSilently(cert.getCustomerId(), use.getCertificateUseId());

            var emp    = employeeDirectory.findById(customer.getCustomerId());
            var roles  = emp.map(EmployeeDirectoryProperties.EmployeeEntry::roles).orElse(List.of("ROLE_CUSTOMER"));
            var branch = emp.map(EmployeeDirectoryProperties.EmployeeEntry::branch).orElse(null);
            var grade  = emp.map(EmployeeDirectoryProperties.EmployeeEntry::grade).orElse(null);

            String accessToken  = jwtProvider.generateAccessToken(
                    customer.getCustomerId(), customer.getEmail(), roles, branch, grade);
            String refreshToken = jwtProvider.generateRefreshToken(customer.getCustomerId());

            redisTemplate.opsForValue().set(
                    RT_KEY_PREFIX + customer.getCustomerId(),
                    sha256(refreshToken),
                    Duration.ofMillis(jwtProperties.refreshTokenValidity()));

            return new LoginResponse(customer.getCustomerId(), accessToken, refreshToken);

        } catch (BusinessException e) {
            // PIN 실패 이외의 실패(만료, 폐기, 잠금 선제 확인)는 여기서 이력 저장
            if (cert.getCertificateStatusCode() != null) {
                String resultCode = resolveFailResultCode(e);
                if (resultCode != null) {
                    saveCertUse(cert, ip, resultCode, e.getErrorCode().getCode());
                }
            }
            throw e;
        }
    }

    private CertificateUse saveCertUse(Certificate cert, String ip, String resultCode, String failureReason) {
        // MVP: signedDataHash = serial + ip + timestamp 해시, signatureValue = certPinHash 또는 "N/A"
        String signedData = sha256(cert.getCertificateSerialNumber() + ip + OffsetDateTime.now());
        String sigValue   = cert.getCertPinHash() != null ? cert.getCertPinHash() : "N/A";

        return certificateUseRepository.save(CertificateUse.builder()
                .certificateId(cert.getCertificateId())
                .customerId(cert.getCustomerId())
                .purposeCode(CertificateUse.PURPOSE_LOGIN)
                .signedDataHash(signedData)
                .signatureValue(sigValue)
                .verificationResultCode(resultCode)
                .failureReasonCode(failureReason)
                .requestIp(ip)
                .requestChannelCode(CertificateUse.CHANNEL_WEB)
                .usedAt(OffsetDateTime.now())
                .build());
    }

    /** FDS 평가 — 성공 경로용. BLOCK 룰이 발동해도 로그인 자체는 막지 않고 로그만 남긴다. */
    private void evaluateFdsSilently(Long customerId, Long referenceId) {
        try {
            fdsService.evaluate(customerId, FdsDetection.EVENT_CERT_LOGIN, referenceId);
        } catch (BusinessException e) {
            log.warn("FDS BLOCK 발동 (인증서 로그인 성공 경로): customerId={}", customerId);
        }
    }

    /** BusinessException 코드에서 certificate_use 결과 코드 매핑. 이미 saveCertUse를 호출한 경우 null 반환. */
    private String resolveFailResultCode(BusinessException e) {
        String code = e.getErrorCode().getCode();
        return switch (code) {
            case "CUST_031" -> CertificateUse.RESULT_FAIL_EXPIRED;
            case "CUST_032" -> CertificateUse.RESULT_FAIL_REVOKED;
            case "CUST_034" -> null; // PIN 실패 경로에서 이미 저장
            default         -> null;
        };
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
