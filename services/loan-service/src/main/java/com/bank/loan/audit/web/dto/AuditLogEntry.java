package com.bank.loan.audit.web.dto;

import com.bank.loan.audit.domain.AccessAuditLog;

import java.time.OffsetDateTime;

public record AuditLogEntry(
        Long logId,
        Long actorId,
        String targetType,
        Long targetId,
        String actionCd,
        String branchId,
        String breakGlassReason,
        OffsetDateTime loggedAt
) {
    public static AuditLogEntry from(AccessAuditLog log) {
        return new AuditLogEntry(
                log.getLogId(),
                log.getActorId(),
                log.getTargetType(),
                log.getTargetId(),
                log.getActionCd(),
                log.getBranchId(),
                log.getBreakGlassReason(),
                log.getLoggedAt()
        );
    }
}
