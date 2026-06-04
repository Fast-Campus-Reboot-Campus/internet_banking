package com.bank.customer.pin.domain;

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

@Entity
@Table(name = "pin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Pin extends BaseEntity {

    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_LOCKED   = "LOCKED";
    public static final String STATUS_REVOKED  = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pin_id")
    private Long pinId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "auth_method_id", nullable = false)
    private Long authMethodId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Column(name = "pin_length", nullable = false)
    private int pinLength;

    @Column(name = "pin_login_failure_count", nullable = false)
    private int pinLoginFailureCount;

    @Column(name = "max_pin_login_failure_count", nullable = false)
    private int maxPinLoginFailureCount;

    @Column(name = "pin_login_locked_at")
    private OffsetDateTime pinLoginLockedAt;

    @Column(name = "pin_login_unlocked_at")
    private OffsetDateTime pinLoginUnlockedAt;

    @Column(name = "pin_last_login_at")
    private OffsetDateTime pinLastLoginAt;

    @Column(name = "pin_status_code", nullable = false, length = 20)
    private String pinStatusCode;

    public boolean isActive() { return STATUS_ACTIVE.equals(pinStatusCode); }
    public boolean isLocked() { return STATUS_LOCKED.equals(pinStatusCode); }

    public void recordLoginSuccess() {
        this.pinLoginFailureCount = 0;
        this.pinLastLoginAt       = OffsetDateTime.now();
    }

    public void recordLoginFailure() {
        this.pinLoginFailureCount++;
        if (this.pinLoginFailureCount >= this.maxPinLoginFailureCount) {
            this.pinStatusCode    = STATUS_LOCKED;
            this.pinLoginLockedAt = OffsetDateTime.now();
        }
    }

    public void unlock() {
        this.pinStatusCode         = STATUS_ACTIVE;
        this.pinLoginFailureCount  = 0;
        this.pinLoginUnlockedAt    = OffsetDateTime.now();
    }

    public void revoke() { this.pinStatusCode = STATUS_REVOKED; }

    public void changePin(String newPinHash) {
        this.pinHash              = newPinHash;
        this.pinLoginFailureCount = 0;
    }
}
