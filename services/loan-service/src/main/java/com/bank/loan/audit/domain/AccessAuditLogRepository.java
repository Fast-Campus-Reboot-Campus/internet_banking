package com.bank.loan.audit.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, Long> {

    List<AccessAuditLog> findByActorIdOrderByLoggedAtDesc(Long actorId);

    List<AccessAuditLog> findByTargetTypeAndTargetIdOrderByLoggedAtDesc(String targetType, Long targetId);

    List<AccessAuditLog> findByActionCdOrderByLoggedAtDesc(String actionCd);

    List<AccessAuditLog> findByActorIdAndActionCdOrderByLoggedAtDesc(Long actorId, String actionCd);
}
