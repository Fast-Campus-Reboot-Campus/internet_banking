package com.bank.common.security.jwt;

import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 생성·파싱·검증 유틸.
 * JwtConfig 에 의해 빈으로 등록되며, jwt.secret 프로퍼티가 없으면 빈이 생성되지 않는다.
 */
public class JwtProvider {

    private static final String CLAIM_EMAIL      = "email";
    private static final String CLAIM_ROLES      = "roles";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String CLAIM_BRANCH     = "branch";
    private static final String CLAIM_GRADE      = "grade";

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity  = properties.accessTokenValidity();
        this.refreshTokenValidity = properties.refreshTokenValidity();
    }

    public String generateAccessToken(Long customerId, String email, List<String> roles) {
        return generateAccessToken(customerId, email, roles, null, null);
    }

    /** 직원 로그인 시 지점(branch)·직급(grade) claim 포함 버전. */
    public String generateAccessToken(Long customerId, String email, List<String> roles,
                                      String branch, String grade) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(String.valueOf(customerId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, TokenType.ACCESS.name());
        if (branch != null) builder.claim(CLAIM_BRANCH, branch);
        if (grade  != null) builder.claim(CLAIM_GRADE,  grade);
        return builder
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenValidity))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long customerId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(customerId))
                .claim(CLAIM_TOKEN_TYPE, TokenType.REFRESH.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenValidity))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰을 파싱해 클레임을 반환한다. 만료·서명 오류 시 JwtException 을 던진다.
     */
    public JwtClaims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long customerId = Long.parseLong(claims.getSubject());
        String email    = claims.get(CLAIM_EMAIL, String.class);

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(CLAIM_ROLES, List.class);

        TokenType tokenType = TokenType.valueOf(claims.get(CLAIM_TOKEN_TYPE, String.class));
        String branch = claims.get(CLAIM_BRANCH, String.class);
        String grade  = claims.get(CLAIM_GRADE,  String.class);

        return new JwtClaims(customerId, email, roles != null ? roles : List.of(), tokenType, branch, grade);
    }

    /**
     * 서명·만료를 검증한다. 실패 시 원인별 BusinessException 을 던진다.
     */
    public void validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }
    }
}
