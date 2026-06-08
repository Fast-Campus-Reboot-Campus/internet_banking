package com.bank.customer.login.service;

import com.bank.common.security.jwt.JwtProperties;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.login.dto.LoginRequest;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.login.dto.RefreshRequest;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginServiceTest {

    @Mock CredentialRepository credentialRepository;
    @Mock CustomerRepository   customerRepository;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock StringRedisTemplate  redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock AuthEventService     authEventService;

    private JwtProvider   jwtProvider;
    private JwtProperties jwtProperties;

    private LoginService loginService;

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(SECRET, 600_000L, 3_600_000L);
        jwtProvider   = new JwtProvider(jwtProperties);

        loginService = new LoginService(
                credentialRepository, customerRepository,
                passwordEncoder, jwtProvider, redisTemplate, authEventService);

        // 후처리는 AuthEventService 단위 책임 — 여기서는 위임 결과만 스텁한다.
        given(authEventService.onLoginSuccess(any(), anyString(), any(), any(), anyString()))
                .willReturn(new LoginResponse(1L, "access-token", "refresh-token"));
        given(authEventService.reissueTokens(any()))
                .willReturn(new LoginResponse(1L, "new-access-token", "new-refresh-token"));
    }

    // ── 로그인 성공 ───────────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공 — 검증 통과 후 AuthEventService 후처리 결과를 반환")
    void loginSuccess() {
        Credential credential = mockCredential(false, true, false);
        Customer   customer   = mockCustomer();

        given(credentialRepository.findByLoginIdAndDeletedAtIsNull("user1"))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("pass1234", "hashed")).willReturn(true);
        given(customerRepository.findByCustomerIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(customer));

        LoginResponse response = loginService.login(new LoginRequest("user1", "pass1234"), "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.customerId()).isEqualTo(1L);
        verify(authEventService).onLoginSuccess(customer, "user1", "127.0.0.1", "JUnit", AuthEventService.CHANNEL_WEB);
    }

    @Test
    @DisplayName("로그인 실패 — 존재하지 않는 ID")
    void loginFailUnknownId() {
        given(credentialRepository.findByLoginIdAndDeletedAtIsNull("unknown"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginRequest("unknown", "pass"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CustomerErrorCode.CUST_010));

        // 실패 시도도 후처리(이력·FDS)에 위임된다
        verify(authEventService).onLoginFailure(anyString(), any(), anyString(), anyString(),
                anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    @DisplayName("로그인 실패 — 비밀번호 불일치")
    void loginFailWrongPassword() {
        Credential credential = mockCredential(false, true, false);
        given(credentialRepository.findByLoginIdAndDeletedAtIsNull("user1"))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user1", "wrong"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("로그인 실패 — 잠긴 계정")
    void loginFailLockedAccount() {
        Credential credential = mockCredential(true, true, false);
        given(credentialRepository.findByLoginIdAndDeletedAtIsNull("user1"))
                .willReturn(Optional.of(credential));

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user1", "pass"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CustomerErrorCode.CUST_011));
    }

    // ── Refresh Token Rotation ────────────────────────────────────

    @Test
    @DisplayName("refresh 성공 — 새 토큰 쌍 반환")
    void refreshSuccess() {
        String oldRefreshToken = jwtProvider.generateRefreshToken(1L);
        String storedHash      = sha256(oldRefreshToken);
        Customer customer      = mockCustomer();

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("RT:1")).willReturn(storedHash);
        given(customerRepository.findByCustomerIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(customer));

        LoginResponse response = loginService.refresh(new RefreshRequest(oldRefreshToken));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo(oldRefreshToken);
        verify(redisTemplate).delete("RT:1");
        verify(authEventService).reissueTokens(customer);
    }

    @Test
    @DisplayName("refresh 실패 — Redis에 없는 토큰 (재사용 시도)")
    void refreshFailTokenReuse() {
        String refreshToken = jwtProvider.generateRefreshToken(1L);

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("RT:1")).willReturn(null);

        assertThatThrownBy(() -> loginService.refresh(new RefreshRequest(refreshToken)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.TOKEN_INVALID));
    }

    @Test
    @DisplayName("refresh 실패 — ACCESS 토큰으로 refresh 시도")
    void refreshFailWithAccessToken() {
        String accessToken = jwtProvider.generateAccessToken(1L, "test@bank.com",
                java.util.List.of("ROLE_CUSTOMER"));

        assertThatThrownBy(() -> loginService.refresh(new RefreshRequest(accessToken)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.TOKEN_INVALID));
    }

    // ── helpers ──────────────────────────────────────────────────

    private Credential mockCredential(boolean locked, boolean active, boolean expired) {
        Credential c = org.mockito.Mockito.mock(Credential.class);
        given(c.isLocked()).willReturn(locked);
        given(c.isActive()).willReturn(active);
        given(c.isPasswordExpired()).willReturn(expired);
        given(c.getCustomerId()).willReturn(1L);
        given(c.getPasswordHash()).willReturn("hashed");
        return c;
    }

    private Customer mockCustomer() {
        Customer c = org.mockito.Mockito.mock(Customer.class);
        given(c.getCustomerId()).willReturn(1L);
        given(c.getEmail()).willReturn("test@bank.com");
        given(c.isActive()).willReturn(true);
        return c;
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
