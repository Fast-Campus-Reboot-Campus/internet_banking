package com.bank.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 생성·파싱·검증 유틸.
 * JwtConfig 에 의해 빈으로 등록되며, jwt.secret 프로퍼티가 없으면 빈이 생성되지 않는다.
 */
public class JwtProvider {

    private static final String CLAIM_EMAIL      = "email";
    private static final String CLAIM_ROLES      = "roles";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity  = properties.accessTokenValidity();
        this.refreshTokenValidity = properties.refreshTokenValidity();
    }

    public String generateAccessToken(Long customerId, String email, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(customerId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, TokenType.ACCESS.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenValidity))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long customerId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
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

        return new JwtClaims(customerId, email, roles != null ? roles : List.of(), tokenType);
    }

    /**
     * 서명·만료를 검증한다. 유효하면 true, 그 외 false.
     */
    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
