package com.bank.customer.fds.domain;

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
 * FDS 탐지 결과 (fds_detection 테이블).
 * INSERT-only — 탐지 발생 시 생성, 이후 상태(CONFIRMED/FALSE_POSITIVE)는 UPDATE.
 */
@Entity
@Table(name = "fds_detection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FdsDetection {

    public static final String STATUS_PENDING        = "PENDING";
    public static final String STATUS_CONFIRMED      = "CONFIRMED";
    public static final String STATUS_FALSE_POSITIVE = "FALSE_POSITIVE";

    public static final String EVENT_LOGIN_ATTEMPT  = "LOGIN_ATTEMPT";
    public static final String EVENT_CERT_LOGIN     = "CERT_LOGIN";
    public static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fds_detection_id")
    private Long fdsDetectionId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "fds_rule_id", nullable = false)
    private Long fdsRuleId;

    @Column(name = "fds_detection_event_type_code", nullable = false, length = 30)
    private String fdsDetectionEventTypeCode;

    /** 이벤트 원본 ID (login_attempt_id, certificate_use_id 등) */
    @Column(name = "fds_detection_event_reference_id", nullable = false)
    private Long fdsDetectionEventReferenceId;

    @Column(name = "fds_detected_at", nullable = false)
    private OffsetDateTime fdsDetectedAt;

    @Column(name = "fds_detection_status_code", nullable = false, length = 20)
    private String fdsDetectionStatusCode;

    public void confirm()       { this.fdsDetectionStatusCode = STATUS_CONFIRMED; }
    public void markFalsePositive() { this.fdsDetectionStatusCode = STATUS_FALSE_POSITIVE; }
    public boolean isPending()  { return STATUS_PENDING.equals(fdsDetectionStatusCode); }
}
