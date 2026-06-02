package com.bank.loan.audit.web;

import com.bank.loan.audit.service.AuditLogService;
import com.bank.loan.audit.web.dto.AuditLogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    /** break-glass 이벤트 전체 조회 (COMPLIANCE 전용). actorId 로 필터 가능. */
    @GetMapping("/break-glass")
    public List<AuditLogEntry> listBreakGlass(
            @RequestParam(required = false) Long actorId) {
        return service.listBreakGlass(actorId);
    }

    /** 특정 건의 전체 접근 이력 조회 (COMPLIANCE 전용). */
    @GetMapping("/access-logs")
    public List<AuditLogEntry> listByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return service.listByTarget(targetType, targetId);
    }
}
