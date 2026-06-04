package com.bank.loan.audit.service;

import com.bank.loan.audit.domain.AccessAuditLog;
import com.bank.loan.audit.domain.AccessAuditLogRepository;
import com.bank.loan.audit.web.dto.AuditLogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditLogService {

    private final AccessAuditLogRepository repository;

    public List<AuditLogEntry> listBreakGlass(Long actorId) {
        List<AccessAuditLog> logs = actorId != null
                ? repository.findByActorIdAndActionCdOrderByLoggedAtDesc(actorId, AccessAuditLog.ACTION_BREAK_GLASS)
                : repository.findByActionCdOrderByLoggedAtDesc(AccessAuditLog.ACTION_BREAK_GLASS);
        return logs.stream().map(AuditLogEntry::from).toList();
    }

    public List<AuditLogEntry> listByTarget(String targetType, Long targetId) {
        return repository
                .findByTargetTypeAndTargetIdOrderByLoggedAtDesc(targetType, targetId)
                .stream()
                .map(AuditLogEntry::from)
                .toList();
    }
}
