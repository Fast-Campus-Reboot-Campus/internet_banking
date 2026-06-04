package com.bank.customer.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * API 토큰 이력 (api_token 테이블).
 * 실제 토큰 검증은 Redis + JWT로 처리하고, DB에는 감사 이력을 남긴다.
 */
@Entity
@Table(name = "api_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiToken {

    public static final String TYPE_ACCESS  = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    public static final String CHANNEL_WEB = "WEB";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** DEFERRABLE FK → login_session */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "token_type_code", nullable = false, length = 20)
    private String tokenTypeCode;

    /** SHA-256 해시 — 원문 JWT는 저장하지 않는다 */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "token_issued_channel_code", nullable = false, length = 20)
    private String tokenIssuedChannelCode;

    @Column(name = "token_scope", length = 500)
    private String tokenScope;

    @Column(name = "token_issued_at", nullable = false)
    private OffsetDateTime tokenIssuedAt;

    @Column(name = "token_expiry_at", nullable = false)
    private OffsetDateTime tokenExpiryAt;

    @Column(name = "token_revoked_at")
    private OffsetDateTime tokenRevokedAt;

    @Column(name = "token_revoke_reason_code", length = 20)
    private String tokenRevokeReasonCode;

    public void revoke(String reason) {
        this.tokenRevokedAt        = OffsetDateTime.now();
        this.tokenRevokeReasonCode = reason;
    }

    public boolean isRevoked() { return tokenRevokedAt != null; }
}
