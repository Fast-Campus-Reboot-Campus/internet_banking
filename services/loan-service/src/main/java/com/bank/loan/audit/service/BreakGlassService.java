package com.bank.loan.audit.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.audit.domain.AccessAuditLog;
import com.bank.loan.audit.domain.AccessAuditLogRepository;
import com.bank.loan.audit.store.BreakGlassGrantStore;
import com.bank.loan.audit.web.dto.BreakGlassRequest;
import com.bank.loan.audit.web.dto.BreakGlassResponse;
import com.bank.loan.security.LoanActorContext;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class BreakGlassService {

    private static final Duration GRANT_TTL = Duration.ofHours(1);
    private static final int MIN_REASON_LENGTH = 10;

    private final BreakGlassGrantStore grantStore;
    private final AccessAuditLogRepository auditLogRepository;
    private final LoanApplicationRepository applicationRepository;

    public BreakGlassResponse execute(BreakGlassRequest request, LoanActorContext actor) {
        if (request.getReason() == null || request.getReason().trim().length() < MIN_REASON_LENGTH) {
            throw new BusinessException(LoanErrorCode.LOAN_205);
        }

        applicationRepository.findByApplIdAndDeletedAtIsNull(request.getApplId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_206));

        OffsetDateTime now = OffsetDateTime.now();

        grantStore.grant(actor.actorId(), AccessAuditLog.TARGET_LOAN_APPLICATION, request.getApplId(), GRANT_TTL);

        AccessAuditLog log = AccessAuditLog.builder()
                .actorId(actor.actorId())
                .targetType(AccessAuditLog.TARGET_LOAN_APPLICATION)
                .targetId(request.getApplId())
                .actionCd(AccessAuditLog.ACTION_BREAK_GLASS)
                .branchId(actor.branch())
                .breakGlassReason(request.getReason().trim())
                .loggedAt(now)
                .build();

        auditLogRepository.save(log);

        return new BreakGlassResponse(log.getLogId(), now.plus(GRANT_TTL));
    }
}
