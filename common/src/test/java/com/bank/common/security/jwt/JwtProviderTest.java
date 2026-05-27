package com.bank.common.security.jwt;

import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";
    private static final long ACCESS_VALIDITY  = 600_000L;   // 10분
    private static final long REFRESH_VALIDITY = 3_600_000L; // 1시간

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, ACCESS_VALIDITY, REFRESH_VALIDITY);
        jwtProvider = new JwtProvider(props);
    }

    @Test
    @DisplayName("ACCESS 토큰 생성 후 claims 파싱 성공")
    void generateAndParseAccessToken() {
        String token = jwtProvider.generateAccessToken(1L, "test@bank.com", List.of("ROLE_CUSTOMER"));

        JwtClaims claims = jwtProvider.parseClaims(token);

        assertThat(claims.customerId()).isEqualTo(1L);
        assertThat(claims.email()).isEqualTo("test@bank.com");
        assertThat(claims.roles()).containsExactly("ROLE_CUSTOMER");
        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
    }

    @Test
    @DisplayName("REFRESH 토큰 생성 후 claims 파싱 성공")
    void generateAndParseRefreshToken() {
        String token = jwtProvider.generateRefreshToken(1L);

        JwtClaims claims = jwtProvider.parseClaims(token);

        assertThat(claims.customerId()).isEqualTo(1L);
        assertThat(claims.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("validate — 유효한 토큰은 예외 없이 통과")
    void validateValidToken() {
        String token = jwtProvider.generateAccessToken(1L, "test@bank.com", List.of("ROLE_CUSTOMER"));

        // 예외 없이 통과하면 성공
        jwtProvider.validate(token);
    }

    @Test
    @DisplayName("validate — 만료된 토큰은 TOKEN_EXPIRED 예외")
    void validateExpiredToken() {
        JwtProperties expiredProps = new JwtProperties(SECRET, -1L, REFRESH_VALIDITY); // 이미 만료
        JwtProvider expiredProvider = new JwtProvider(expiredProps);
        String token = expiredProvider.generateAccessToken(1L, "test@bank.com", List.of("ROLE_CUSTOMER"));

        assertThatThrownBy(() -> jwtProvider.validate(token))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("validate — 위조된 토큰은 TOKEN_INVALID 예외")
    void validateTamperedToken() {
        String token = jwtProvider.generateAccessToken(1L, "test@bank.com", List.of("ROLE_CUSTOMER"));
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtProvider.validate(tampered))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.TOKEN_INVALID));
    }

    @Test
    @DisplayName("validate — 완전히 잘못된 문자열은 TOKEN_INVALID 예외")
    void validateGarbageToken() {
        assertThatThrownBy(() -> jwtProvider.validate("not.a.jwt"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.TOKEN_INVALID));
    }
}
