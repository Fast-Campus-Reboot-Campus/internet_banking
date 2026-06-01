package com.bank.customer.cert.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "qr_login_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QrLoginToken {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_SCANNED   = "SCANNED";
    public static final String STATUS_APPROVED  = "APPROVED";
    public static final String STATUS_EXPIRED   = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_token_id")
    private Long qrTokenId;

    @Column(name = "qr_token_hash", nullable = false, unique = true, length = 255)
    private String qrTokenHash;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "qr_status_code", nullable = false, length = 20)
    private String qrStatusCode;

    @Column(name = "request_ip", nullable = false, length = 45)
    private String requestIp;

    @Column(name = "request_channel_code", nullable = false, length = 20)
    private String requestChannelCode;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expiry_at", nullable = false)
    private OffsetDateTime expiryAt;

    @Column(name = "scanned_at")
    private OffsetDateTime scannedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    // -------------------------------------------------------------------------

    public boolean isPending() {
        return STATUS_PENDING.equals(qrStatusCode);
    }

    public boolean isExpiredByTime() {
        return OffsetDateTime.now().isAfter(expiryAt);
    }

    public void markScanned() {
        this.qrStatusCode = STATUS_SCANNED;
        this.scannedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void approve(Long customerId) {
        this.qrStatusCode = STATUS_APPROVED;
        this.customerId = customerId;
        this.approvedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void expire() {
        this.qrStatusCode = STATUS_EXPIRED;
        this.updatedAt = OffsetDateTime.now();
    }
}
