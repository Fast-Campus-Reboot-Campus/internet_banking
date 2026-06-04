package com.bank.customer.history.domain;

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
 * 비밀번호 변경 이력 (password_history 테이블).
 * INSERT-only 감사 테이블 — BaseEntity 미적용 (created_by NOT NULL 회피).
 */
@Entity
@Table(name = "password_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_history_id")
    private Long passwordHistoryId;

    @Column(name = "credential_id", nullable = false)
    private Long credentialId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** 변경 직전 비밀번호 해시 — 재사용 방지 검증에 활용 */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_change_channel_code", nullable = false, length = 20)
    private String passwordChangeChannelCode;

    @Column(name = "password_change_reason_code", length = 200)
    private String passwordChangeReasonCode;

    @Column(name = "password_change_ip", length = 45)
    private String passwordChangeIp;

    /** DB DEFAULT CURRENT_TIMESTAMP — INSERT 시 자동 설정, 조회 전용 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
