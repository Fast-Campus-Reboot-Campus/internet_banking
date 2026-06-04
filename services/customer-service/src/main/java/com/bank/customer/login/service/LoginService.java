package com.bank.customer.login.service;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProperties;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import com.bank.customer.config.EmployeeDirectoryProperties;
import com.bank.customer.config.EmployeeDirectoryProperties.EmployeeEntry;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.fds.domain.FdsDetection;
import com.bank.customer.fds.service.FdsService;
import com.bank.customer.login.domain.LoginAttempt;
import com.bank.customer.session.service.LoginSessionService;
import com.bank.customer.login.dto.LoginRequest;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.login.dto.RefreshRequest;
import com.bank.customer.login.repository.LoginAttemptRepository;
import com.bank.customer.support.CustomerErrorCode;
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
public class LoginService {

    private static final String RT_KEY_PREFIX = "RT:";
    private static final String CHANNEL_WEB   = "WEB";

    private final CredentialRepository     credentialRepository;
    private final CustomerRepository       customerRepository;
    private final PasswordEncoder          passwordEncoder;
    private final JwtProvider              jwtProvider;
    private final JwtProperties            jwtProperties;
    private final StringRedisTemplate      redisTemplate;
    private final EmployeeDirectoryProperties employeeDirectory;
    private final LoginAttemptRepository   loginAttemptRepository;
    private final FdsService               fdsService;
    private final LoginSessionService      loginSessionService;

    /**
     * 아이디/비밀번호 로그인.
     * noRollbackFor: 실패 카운트·잠금 상태와 감사 로그는 예외 시에도 반드시 커밋돼야 한다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        Long customerId = null;

        try {
            Credential credential = credentialRepository
                    .findByLoginIdAndDeletedAtIsNull(request.loginId())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));

            customerId = credential.getCustomerId();

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

            String accessToken  = buildAccessToken(customer);
            String refreshToken = jwtProvider.generateRefreshToken(customer.getCustomerId());

            storeRefreshToken(customer.getCustomerId(), refreshToken);
            LoginAttempt attempt = saveAttempt(request.loginId(), customer.getCustomerId(), ip, userAgent, true, null);

            // 세션·토큰 DB 이력 저장 (Redis JWT와 병행)
            loginSessionService.createSession(
                    customer.getCustomerId(), attempt.getLoginAttemptId(), ip,
                    accessToken, refreshToken,
                    OffsetDateTime.now().plusSeconds(jwtProperties.accessTokenValidity()  / 1000),
                    OffsetDateTime.now().plusSeconds(jwtProperties.refreshTokenValidity() / 1000));

            // 로그인 성공 시에도 FDS 평가 — 이상 패턴 모니터링
            evaluateFdsSilently(customer.getCustomerId(), FdsDetection.EVENT_LOGIN_ATTEMPT, attempt.getLoginAttemptId());

            return new LoginResponse(customer.getCustomerId(), accessToken, refreshToken);

        } catch (BusinessException e) {
            LoginAttempt attempt = saveAttempt(request.loginId(), customerId, ip, userAgent, false,
                    e.getErrorCode().getCode());

            // 로그인 실패 시 FDS 평가 — BLOCK 룰이 발동하면 CUST_060 으로 재던짐
            if (customerId != null) {
                evaluateFds(customerId, FdsDetection.EVENT_LOGIN_ATTEMPT, attempt.getLoginAttemptId());
            }
            throw e;
        }
    }

    @Transactional
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
        String key      = RT_KEY_PREFIX + customerId;
        String stored   = redisTemplate.opsForValue().get(key);

        if (stored == null || !stored.equals(sha256(request.refreshToken()))) {
            redisTemplate.delete(key);
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }

        redisTemplate.delete(key);

        Customer customer = customerRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        String newAccessToken  = buildAccessToken(customer);
        String newRefreshToken = jwtProvider.generateRefreshToken(customerId);

        storeRefreshToken(customerId, newRefreshToken);

        return new LoginResponse(customerId, newAccessToken, newRefreshToken);
    }

    private String buildAccessToken(Customer customer) {
        return employeeDirectory.find(customer.getCustomerId())
                .map(emp -> jwtProvider.generateAccessToken(
                        customer.getCustomerId(), customer.getEmail(),
                        emp.roles(), emp.branch(), emp.grade()))
                .orElseGet(() -> jwtProvider.generateAccessToken(
                        customer.getCustomerId(), customer.getEmail(),
                        List.of("ROLE_CUSTOMER")));
    }

    private void storeRefreshToken(Long customerId, String refreshToken) {
        redisTemplate.opsForValue().set(
                RT_KEY_PREFIX + customerId,
                sha256(refreshToken),
                Duration.ofMillis(jwtProperties.refreshTokenValidity()));
    }

    private LoginAttempt saveAttempt(String loginId, Long customerId, String ip, String userAgent,
                                     boolean success, String failureCode) {
        return loginAttemptRepository.save(LoginAttempt.builder()
                .attemptedLoginId(loginId)
                .customerId(customerId)
                .loginAttemptChannelCode(CHANNEL_WEB)
                .loginAttemptIp(ip)
                .loginAttemptUserAgent(userAgent)
                .loginAttemptSuccessYn(success ? "T" : "F")
                .loginAttemptFailureReasonCode(failureCode)
                .loginAttemptedAt(OffsetDateTime.now())
                .build());
    }

    /** FDS 평가 — BLOCK 룰 발동 시 예외를 그대로 전파한다. */
    private void evaluateFds(Long customerId, String eventType, Long referenceId) {
        fdsService.evaluate(customerId, eventType, referenceId);
    }

    /** FDS 평가 — 성공 경로에서 호출. BLOCK 룰이 발동해도 로그만 남기고 예외를 삼킨다. */
    private void evaluateFdsSilently(Long customerId, String eventType, Long referenceId) {
        try {
            fdsService.evaluate(customerId, eventType, referenceId);
        } catch (BusinessException e) {
            log.warn("FDS BLOCK 발동 (성공 경로): customerId={} eventType={}", customerId, eventType);
        }
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
