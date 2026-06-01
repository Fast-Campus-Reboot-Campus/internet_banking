package com.bank.customer.login.service;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProperties;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.login.config.EmployeeDirectoryProperties;
import com.bank.customer.login.dto.LoginRequest;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.login.dto.RefreshRequest;
import com.bank.customer.support.CustomerErrorCode;
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
public class LoginService {

    private static final String RT_KEY_PREFIX = "RT:";

    private final CredentialRepository credentialRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final EmployeeDirectoryProperties employeeDirectory;

    /**
     * noRollbackFor: 비밀번호 실패 카운트·잠금 상태는 예외 발생 시에도 반드시 커밋돼야 한다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request) {

        Credential credential = credentialRepository
                .findByLoginIdAndDeletedAtIsNull(request.loginId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));

        if (credential.isLocked()) {
            throw new BusinessException(CustomerErrorCode.CUST_011);
        }
        if (!credential.isActive()) {
            throw new BusinessException(CustomerErrorCode.CUST_012);
        }
        if (credential.isPasswordExpired()) {
            throw new BusinessException(CustomerErrorCode.CUST_013);
        }

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            credential.recordLoginFailure();
            // 임계치 도달로 잠금 전환된 경우 잠금 오류 코드로 응답
            throw new BusinessException(
                    credential.isLocked() ? CustomerErrorCode.CUST_011 : CustomerErrorCode.CUST_010);
        }

        Customer customer = customerRepository
                .findByCustomerIdAndDeletedAtIsNull(credential.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!customer.isActive()) {
            throw new BusinessException(CustomerErrorCode.CUST_012);
        }

        credential.recordLoginSuccess();

        var emp = employeeDirectory.findById(customer.getCustomerId());
        var roles  = emp.map(EmployeeDirectoryProperties.EmployeeEntry::roles).orElse(List.of("ROLE_CUSTOMER"));
        var branch = emp.map(EmployeeDirectoryProperties.EmployeeEntry::branch).orElse(null);
        var grade  = emp.map(EmployeeDirectoryProperties.EmployeeEntry::grade).orElse(null);

        String accessToken  = jwtProvider.generateAccessToken(
                customer.getCustomerId(), customer.getEmail(), roles, branch, grade);
        String refreshToken = jwtProvider.generateRefreshToken(customer.getCustomerId());

        storeRefreshToken(customer.getCustomerId(), refreshToken);

        return new LoginResponse(customer.getCustomerId(), accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        JwtClaims claims;
        try {
            claims = jwtProvider.parseClaims(request.refreshToken());
        } catch (BusinessException e) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }

        if (claims.tokenType() != TokenType.REFRESH) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }

        Long customerId = claims.customerId();
        String key = RT_KEY_PREFIX + customerId;
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null || !stored.equals(sha256(request.refreshToken()))) {
            redisTemplate.delete(key); // 토큰 재사용 시도 → 강제 무효화
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }

        redisTemplate.delete(key);

        Customer customer = customerRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        var emp2   = employeeDirectory.findById(customerId);
        var roles2  = emp2.map(EmployeeDirectoryProperties.EmployeeEntry::roles).orElse(List.of("ROLE_CUSTOMER"));
        var branch2 = emp2.map(EmployeeDirectoryProperties.EmployeeEntry::branch).orElse(null);
        var grade2  = emp2.map(EmployeeDirectoryProperties.EmployeeEntry::grade).orElse(null);

        String newAccessToken  = jwtProvider.generateAccessToken(
                customerId, customer.getEmail(), roles2, branch2, grade2);
        String newRefreshToken = jwtProvider.generateRefreshToken(customerId);

        storeRefreshToken(customerId, newRefreshToken);

        return new LoginResponse(customerId, newAccessToken, newRefreshToken);
    }

    private void storeRefreshToken(Long customerId, String refreshToken) {
        redisTemplate.opsForValue().set(
                RT_KEY_PREFIX + customerId,
                sha256(refreshToken),
                Duration.ofMillis(jwtProperties.refreshTokenValidity()));
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
