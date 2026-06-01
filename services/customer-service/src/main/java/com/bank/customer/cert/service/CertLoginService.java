package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.Certificate;
import com.bank.customer.cert.dto.CertLoginRequest;
import com.bank.customer.cert.repository.CertificateRepository;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.login.config.EmployeeDirectoryProperties;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.support.CustomerErrorCode;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertLoginService {

    private static final String RT_KEY_PREFIX = "RT:";

    private final CertificateRepository certificateRepository;
    private final CredentialRepository credentialRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final EmployeeDirectoryProperties employeeDirectory;

    /**
     * 인증서 로그인.
     * PIN 검증은 MVP 단계에서 credential.passwordHash 로 위임한다.
     * (공동인증서·금융인증서·AXful인증서 모두 동일 플로우)
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse certLogin(CertLoginRequest request) {

        Certificate cert = certificateRepository
                .findByCertificateSerialNumberAndDeletedAtIsNull(request.certSerialNumber())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_030));

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

        // PIN = 계정 비밀번호 (MVP 단순화)
        var credential = credentialRepository
                .findByCustomerIdAndDeletedAtIsNull(cert.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));

        if (!passwordEncoder.matches(request.pin(), credential.getPasswordHash())) {
            cert.recordLoginFailure();
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
