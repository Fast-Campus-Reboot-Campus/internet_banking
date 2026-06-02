package com.bank.loan.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 조회·열람·break-glass 접근 이벤트 감사 로그. append-only.
 * 상태 전이(status_history)와 분리 — 접근 행위 자체를 기록한다.
 */
@Getter
@Entity
@Table(name = "access_audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessAuditLog {

    public static final String ACTION_VIEW        = "VIEW";
    public static final String ACTION_UNMASK      = "UNMASK";
    public static final String ACTION_BREAK_GLASS = "BREAK_GLASS";

    public static final String TARGET_LOAN_APPLICATION = "LOAN_APPLICATION";
    public static final String TARGET_LOAN_REVIEW      = "LOAN_REVIEW";
    public static final String TARGET_DOCUMENT         = "DOCUMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "action_cd", nullable = false, length = 30)
    private String actionCd;

    @Column(name = "branch_id", length = 10)
    private String branchId;

    @Column(name = "break_glass_reason", columnDefinition = "TEXT")
    private String breakGlassReason;

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt;

    @Builder
    public AccessAuditLog(Long actorId, String targetType, Long targetId,
                          String actionCd, String branchId,
                          String breakGlassReason, OffsetDateTime loggedAt) {
        this.actorId          = actorId;
        this.targetType       = targetType;
        this.targetId         = targetId;
        this.actionCd         = actionCd;
        this.branchId         = branchId;
        this.breakGlassReason = breakGlassReason;
        this.loggedAt         = loggedAt;
    }
}
