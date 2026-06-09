package com.bank.customer.customer.dto;

import com.bank.customer.customer.domain.CustomerAccessLog;

import java.time.OffsetDateTime;

/**
 * 감사로그 화면(1행) 응답. 스냅샷 컬럼을 그대로 노출한다.
 */
public record AccessLogResponse(
        Long customerAccessLogId,
        Long accessorEmployeeId,
        String accessorName,
        String accessorRole,
        String accessorBranchCode,
        Long targetCustomerId,
        String targetCustomerName,
        String accessActionCode,
        String accessReason,
        OffsetDateTime accessedAt) {

    public static AccessLogResponse of(CustomerAccessLog log) {
        return new AccessLogResponse(
                log.getCustomerAccessLogId(),
                log.getAccessorEmployeeId(),
                log.getAccessorName(),
                log.getAccessorRole(),
                log.getAccessorBranchCode(),
                log.getTargetCustomerId(),
                log.getTargetCustomerName(),
                log.getAccessActionCode(),
                log.getAccessReason(),
                log.getAccessedAt());
    }
}
