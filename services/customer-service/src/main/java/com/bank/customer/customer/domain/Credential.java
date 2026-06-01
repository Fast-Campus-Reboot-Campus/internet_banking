package com.bank.customer.customer.domain;

import com.bank.common.persistence.BaseEntity;
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

@Getter
@Entity
@Table(name = "credential")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Credential extends BaseEntity {

    public static final String STATUS_ACTIVE  = "ACTIVE";
    public static final String STATUS_LOCKED  = "LOCKED";
    public static final String STATUS_DORMANT = "DORMANT";
    public static final String STATUS_CLOSED  = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id")
    private Long credentialId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    /** bcrypt 해시값. 평문 비밀번호는 저장하지 않는다. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private OffsetDateTime passwordChangedAt;

    @Column(name = "password_expiry_at")
    private OffsetDateTime passwordExpiryAt;

    @Column(name = "account_status_code", nullable = false, length = 20)
    private String accountStatusCode;

    @Column(name = "password_login_failure_count", nullable = false)
    private int passwordLoginFailureCount;

    @Column(name = "max_password_login_failure_count", nullable = false)
    private int maxPasswordLoginFailureCount;

    @Column(name = "password_login_locked_at")
    private OffsetDateTime passwordLoginLockedAt;

    @Column(name = "password_login_unlocked_at")
    private OffsetDateTime passwordLoginUnlockedAt;

    @Column(name = "password_last_login_at")
    private OffsetDateTime passwordLastLoginAt;

    // -------------------------------------------------------------------------
    // 비밀번호 변경
    // -------------------------------------------------------------------------

    public void changePassword(String newPasswordHash) {
        this.passwordHash             = newPasswordHash;
        this.passwordChangedAt        = OffsetDateTime.now();
        this.passwordLoginFailureCount = 0;
    }

    public void close() {
        this.accountStatusCode = STATUS_CLOSED;
    }

    // -------------------------------------------------------------------------
    // 로그인 성공 / 실패
    // -------------------------------------------------------------------------

    public void recordLoginSuccess() {
        this.passwordLoginFailureCount = 0;
        this.passwordLastLoginAt       = OffsetDateTime.now();
    }

    /** 실패 횟수를 증가시키고, 임계치 도달 시 계정을 잠근다. */
    public void recordLoginFailure() {
        this.passwordLoginFailureCount++;
        if (this.passwordLoginFailureCount >= this.maxPasswordLoginFailureCount) {
            lock();
        }
    }

    // -------------------------------------------------------------------------
    // 잠금 / 잠금 해제
    // -------------------------------------------------------------------------

    private void lock() {
        this.accountStatusCode      = STATUS_LOCKED;
        this.passwordLoginLockedAt  = OffsetDateTime.now();
    }

    public void unlock() {
        this.accountStatusCode           = STATUS_ACTIVE;
        this.passwordLoginFailureCount   = 0;
        this.passwordLoginUnlockedAt     = OffsetDateTime.now();
    }

    // -------------------------------------------------------------------------
    // 상태 조회
    // -------------------------------------------------------------------------

    public boolean isActive() {
        return STATUS_ACTIVE.equals(accountStatusCode);
    }

    public boolean isLocked() {
        return STATUS_LOCKED.equals(accountStatusCode);
    }

    /** 비밀번호 만료 여부. expiryAt 이 설정된 경우에만 판단한다. */
    public boolean isPasswordExpired() {
        return passwordExpiryAt != null && OffsetDateTime.now().isAfter(passwordExpiryAt);
    }
}
