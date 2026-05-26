package com.bank.loan.statushistory.dto;

import com.bank.common.audit.StatusHistory;

import java.time.OffsetDateTime;

public record StatusHistoryResponse(
        Long sthistId,
        String targetDomainCd,
        String targetTableCd,
        Long targetId,
        String beforeStatusCd,
        String afterStatusCd,
        String changeReasonCd,
        String changeRemark,
        OffsetDateTime changedAt,
        Long changedBy
) {
    public static StatusHistoryResponse of(StatusHistory h) {
        return new StatusHistoryResponse(
                h.getSthistId(),
                h.getTargetDomainCd(),
                h.getTargetTableCd(),
                h.getTargetId(),
                h.getBeforeStatusCd(),
                h.getAfterStatusCd(),
                h.getChangeReasonCd(),
                h.getChangeRemark(),
                h.getChangedAt(),
                h.getChangedBy()
        );
    }
}
